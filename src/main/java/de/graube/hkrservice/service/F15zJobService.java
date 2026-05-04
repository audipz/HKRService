package de.graube.hkrservice.service;

import de.graube.hkrservice.dto.JobResult;
import de.graube.hkrservice.entity.F15zJob;
import de.graube.hkrservice.model.F15zTransaction;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Fachliche Service-Schnittstelle fuer den F15Z-Jobprozess.
 */
public interface F15zJobService {

    /**
     * Erstellt einen neuen Job.
     *
     * @return erzeugter Job
     */
    Mono<F15zJob> createJob();

    /**
     * Ergaenzt einen Job um Transaktionen.
     *
     * @param jobId Job-ID
     * @param txs Transaktionen
     * @return Abschlusssignal
     */
    Mono<Void> addTransactions(String jobId, List<F15zTransaction> txs);

    /**
     * Fuehrt die Export-Pipeline aus.
     *
     * @param jobId Job-ID
     * @return Ergebnis der Ausfuehrung
     */
    Mono<JobResult> execute(String jobId);

    /**
     * Liefert den aktuellen Jobzustand.
     *
     * @param jobId Job-ID
     * @return Job
     */
    Mono<F15zJob> getJob(String jobId);

    /**
     * Liefert den aggregierten Ergebnisstatus eines Jobs.
     *
     * @param jobId Job-ID
     * @return Ergebnisdaten
     */
    Mono<JobResult> getResult(String jobId);
}