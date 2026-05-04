package de.graube.hkrservice.service;

import de.graube.hkrservice.dto.JobResult;
import de.graube.hkrservice.dto.JobStatus;
import de.graube.hkrservice.dto.TransaktionStatus;
import de.graube.hkrservice.entity.F15zJob;
import de.graube.hkrservice.generator.F15zGenerator;
import de.graube.hkrservice.parser.F15zFileParser;
import de.graube.hkrservice.repositorys.F15zTransactionRepository;
import de.graube.hkrservice.repositorys.JobRepository;
import de.graube.hkrservice.validation.F15zValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

import static de.graube.hkrservice.dto.JobStatus.*;
import static de.graube.hkrservice.dto.TransaktionStatus.ERROR;
import static de.graube.hkrservice.dto.TransaktionStatus.NEW;

/**
 * Implementierung des reaktiven F15Z-Jobprozesses.
 */
@Service
@RequiredArgsConstructor
public class F15zService implements F15zJobService {

    private final JobRepository jobRepository;
    private final F15zTransactionRepository transactionRepository;

    /**
     * Legt einen neuen Job mit initialem Status CREATED an.
     *
     * @return erzeugter Job
     */
    @Override
    public Mono<F15zJob> createJob() {
        F15zJob job = new F15zJob();
        job.setJobId("F15Z-" + UUID.randomUUID());
        job.setStatus(CREATED);
        job.setCreatedAt(LocalDateTime.now());

        return jobRepository.save(job);
    }

    /**
     * Speichert Transaktionen fuer einen Job in den Status CREATED/COLLECTING.
     *
     * @param jobId Job-ID
     * @param txs Transaktionsliste
     * @return Abschlusssignal
     */
    @Override
    public Mono<Void> addTransactions(String jobId, List<de.graube.hkrservice.model.F15zTransaction> txs) {
        return jobRepository.findById(jobId)
                .switchIfEmpty(Mono.error(new RuntimeException("Job not found")))
                .flatMap(job -> {
                    if (job.getStatus() != CREATED && job.getStatus() != COLLECTING) {
                        return Mono.error(new RuntimeException("Job not in COLLECTING state"));
                    }

                    job.setStatus(COLLECTING);
                    return jobRepository.save(job);
                })
                .onErrorMap(OptimisticLockingFailureException.class,
                        ex -> new RuntimeException("Job wird gerade parallel bearbeitet"))
                .flatMap(savedJob -> Flux.fromIterable(txs)
                        .map(tx -> toEntity(savedJob.getJobId(), tx))
                        .concatMap(transactionRepository::save)
                        .then())
                .onErrorMap(DataIntegrityViolationException.class,
                        ex -> new RuntimeException("Constraint verletzt (z. B. doppelte Belegnummer)", ex));
    }

    /**
     * Fuehrt Generierung, Parsing und Validierung aus und aktualisiert den Jobstatus.
     *
     * @param jobId Job-ID
     * @return aggregiertes Ergebnis
     */
    @Override
    public Mono<JobResult> execute(String jobId) {
        return claimForExecution(jobId)
                .then(transactionRepository.findByJobId(jobId).collectList())
                .flatMap(dbTxs -> Mono.fromCallable(() -> {
                            String file = new F15zGenerator().build(toModelList(dbTxs));
                            F15zFileParser.Parsed parsed = new F15zFileParser().parse(file);
                            new F15zValidator().validate(parsed);
                            return dbTxs;
                        })
                        .subscribeOn(Schedulers.boundedElastic()))
                .flatMap(dbTxs -> updateStatus(jobId, EXPORTED)
                        .then(updateStatus(jobId, SENT))
                        .then(buildResult(jobId, toModelList(dbTxs), SENT)))
                .onErrorResume(ex -> markFailed(jobId)
                        .then(Mono.fromSupplier(() -> {
                            JobResult res = new JobResult(jobId, FAILED);
                            res.setErrorMessages(List.of(ex.getMessage()));
                            return res;
                        })));
    }

    /**
     * Liest den Jobstatus.
     *
     * @param jobId Job-ID
     * @return Job
     */
    @Override
    public Mono<F15zJob> getJob(String jobId) {
        return jobRepository.findById(jobId)
                .switchIfEmpty(Mono.error(new RuntimeException("Job not found")));
    }

    /**
     * Berechnet ein Jobresultat aus Job- und Transaktionsdaten.
     *
     * @param jobId Job-ID
     * @return Ergebnisobjekt
     */
    @Override
    public Mono<JobResult> getResult(String jobId) {
        return jobRepository.findById(jobId)
                .switchIfEmpty(Mono.error(new RuntimeException("Job not found")))
                .zipWith(transactionRepository.findByJobId(jobId).collectList())
                .flatMap(tuple -> buildResult(jobId, toModelList(tuple.getT2()), tuple.getT1().getStatus()));
    }

    /**
     * Reserviert den Job fuer eine exklusive Execute-Ausfuehrung.
     */
    private Mono<F15zJob> claimForExecution(String jobId) {
        return jobRepository.findById(jobId)
                .switchIfEmpty(Mono.error(new RuntimeException("Job not found")))
                .flatMap(job -> {
                    if (job.getStatus() == SENT || job.getStatus() == EXPORTED || job.getStatus() == PROCESSING) {
                        return Mono.error(new RuntimeException("Job already processed"));
                    }
                    if (job.getStatus() != COLLECTING) {
                        return Mono.error(new RuntimeException("Job not ready"));
                    }

                    job.setStatus(PROCESSING);
                    return jobRepository.save(job);
                })
                .onErrorMap(OptimisticLockingFailureException.class,
                        ex -> new RuntimeException("Parallel Execute erkannt - bitte erneut versuchen", ex));
    }

    /**
     * Aktualisiert den Jobstatus.
     */
    private Mono<Void> updateStatus(String jobId, JobStatus status) {
        return jobRepository.findById(jobId)
                .flatMap(job -> {
                    job.setStatus(status);
                    return jobRepository.save(job);
                })
                .then();
    }

    /**
     * Setzt den Jobstatus auf FAILED.
     */
    private Mono<Void> markFailed(String jobId) {
        return jobRepository.findById(jobId)
                .flatMap(job -> {
                    job.setStatus(FAILED);
                    return jobRepository.save(job);
                })
                .then();
    }

    /**
     * Baut ein JobResult aus der Transaktionsliste.
     */
    private Mono<JobResult> buildResult(String jobId, List<de.graube.hkrservice.model.F15zTransaction> txs, JobStatus status) {
        return Mono.fromSupplier(() -> {
            JobResult res = new JobResult(jobId, status);
            res.setTotal(txs.size());
            res.setSuccess((int) txs.stream().filter(tx -> !Objects.equals(tx.getStatus(), ERROR)).count());
            res.setError((int) txs.stream().filter(tx -> Objects.equals(tx.getStatus(), ERROR)).count());
            return res;
        });
    }

    /**
     * Konvertiert API-Transaktionen in persistierbare Entitaeten.
     */
    private de.graube.hkrservice.entity.F15zTransaction toEntity(String jobId, de.graube.hkrservice.model.F15zTransaction tx) {
        de.graube.hkrservice.entity.F15zTransaction entity = new de.graube.hkrservice.entity.F15zTransaction();
        entity.setId(UUID.randomUUID().toString());
        entity.setJobId(jobId);
        entity.setType(tx.getType() == null ? null : tx.getType().name());
        entity.setBelegnummer(tx.getBelegnummer());
        entity.setFaelligkeit(tx.getFaelligkeit());
        entity.setBetrag(tx.getBetrag());
        entity.setIban(tx.getIban());
        entity.setBic(tx.getBic());
        entity.setTitel(tx.getTitel());
        entity.setObjektkonto(tx.getObjektkonto());
        entity.setStatus(tx.getStatus() == null ? NEW.name() : tx.getStatus().name());
        entity.setErrorMessage(tx.getErrorMessage());
        entity.setNewEntity(true);
        return entity;
    }

    /**
     * Konvertiert persistierte Entitaeten in API-Modelle.
     */
    private List<de.graube.hkrservice.model.F15zTransaction> toModelList(List<de.graube.hkrservice.entity.F15zTransaction> dbTxs) {
        return dbTxs.stream().map(db -> {
            de.graube.hkrservice.model.F15zTransaction tx = new de.graube.hkrservice.model.F15zTransaction();
            if (db.getType() != null) {
                tx.setType(de.graube.hkrservice.model.VslType.valueOf(db.getType()));
            }
            tx.setBelegnummer(db.getBelegnummer());
            tx.setFaelligkeit(db.getFaelligkeit());
            tx.setBetrag(db.getBetrag());
            tx.setIban(db.getIban());
            tx.setBic(db.getBic());
            tx.setTitel(db.getTitel());
            tx.setObjektkonto(db.getObjektkonto());
            if (db.getStatus() != null) {
                tx.setStatus(TransaktionStatus.valueOf(db.getStatus()));
            }
            tx.setErrorMessage(db.getErrorMessage());
            return tx;
        }).toList();
    }
}