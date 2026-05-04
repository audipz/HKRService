package de.graube.hkrservice.service;

import de.graube.hkrservice.dto.JobStatus;
import de.graube.hkrservice.entity.F15zJob;
import de.graube.hkrservice.entity.F15zTransaction;
import de.graube.hkrservice.parser.F15zFileParser;
import de.graube.hkrservice.repositorys.F15zTransactionRepository;
import de.graube.hkrservice.repositorys.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static de.graube.hkrservice.dto.TransaktionStatus.CONFIRMED;
import static de.graube.hkrservice.dto.TransaktionStatus.ERROR;

/**
 * Verarbeitet Rueckmeldedateien und schreibt Ruecklaeuferstatus in die Datenbank.
 */
@Service
@RequiredArgsConstructor
public class F15zReturnService {

    private final JobRepository jobRepository;
    private final F15zTransactionRepository transactionRepository;

    /**
     * Verarbeitet den Rueckmeldedatei-Inhalt fuer einen Job.
     *
     * @param jobId Job-ID
     * @param content Dateiinhalt
     * @return Abschlusssignal
     */
    public Mono<Void> processReturnFile(String jobId, String content) {
        F15zFileParser.Parsed parsed = new F15zFileParser().parse(content);

        return jobRepository.findById(jobId)
                .switchIfEmpty(Mono.error(new RuntimeException("Job not found")))
                .flatMap(job -> ensureSent(job)
                        .then(processRecords(jobId, parsed))
                        .then(updateJobIfAllDone(jobId, job)));
    }

    private Mono<Void> ensureSent(F15zJob job) {
        if (job.getStatus() != JobStatus.SENT) {
            return Mono.error(new RuntimeException("Job not in SENT state"));
        }
        return Mono.empty();
    }

    private Mono<Void> processRecords(String jobId, F15zFileParser.Parsed parsed) {
        return Flux.fromIterable(parsed.records)
                .concatMap(record -> transactionRepository.findByJobIdAndBelegnummer(jobId, record.belegnummer)
                        .flatMap(tx -> applyReturnRecord(tx, record))
                        .switchIfEmpty(Mono.empty()))
                .then();
    }

    private Mono<F15zTransaction> applyReturnRecord(F15zTransaction tx, F15zFileParser.Record record) {
        if (isTerminal(tx.getStatus())) {
            return Mono.just(tx);
        }

        if (isSuccess(record.status)) {
            tx.setStatus(CONFIRMED.name());
            tx.setErrorMessage(null);
        } else {
            tx.setStatus(ERROR.name());
            tx.setErrorMessage(record.message);
        }

        return transactionRepository.save(tx);
    }

    private Mono<Void> updateJobIfAllDone(String jobId, F15zJob job) {
        return transactionRepository.findByJobId(jobId)
                .all(tx -> isTerminal(tx.getStatus()))
                .flatMap(allDone -> {
                    if (!allDone) {
                        return Mono.empty();
                    }
                    job.setStatus(JobStatus.COMPLETED);
                    return jobRepository.save(job).then();
                });
    }

    private boolean isTerminal(String status) {
        return CONFIRMED.name().equals(status) || ERROR.name().equals(status);
    }

    private boolean isSuccess(String status) {
        return status != null && (
                "OK".equalsIgnoreCase(status)
                        || "SUCCESS".equalsIgnoreCase(status)
                        || CONFIRMED.name().equalsIgnoreCase(status)
        );
    }
}