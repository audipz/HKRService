package de.graube.hkrservice.controller;


import de.graube.hkrservice.dto.JobResult;
import de.graube.hkrservice.entity.F15zJob;
import de.graube.hkrservice.idempotency.IdempotencyService;
import de.graube.hkrservice.model.F15zTransaction;
import de.graube.hkrservice.service.F15zJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * REST-API fuer den Lebenszyklus eines F15Z-Jobs.
 */
@RestController
@RequestMapping("/jobs")
@RequiredArgsConstructor
public class F15zController {

    private final F15zJobService service;
    private final IdempotencyService idempotencyService;


    /**
     * Legt einen neuen Job an.
     *
     * @return erzeugter Job
     */
    @PostMapping
    public Mono<F15zJob> createJob() {
        return service.createJob();
    }

    /**
     * Fuegt einem Job Transaktionen hinzu und beruecksichtigt optional Idempotenz.
     *
     * @param jobId Job-ID
     * @param txs Transaktionen
     * @param key optionaler Idempotency-Key
     * @return Abschlusssignal
     */
    @PostMapping("/{jobId}/transactions")
    public Mono<Void> addTransactions(
            @PathVariable String jobId,
            @RequestBody List<F15zTransaction> txs,
            @RequestHeader(value = "Idempotency-Key", required = false) String key) {
        Mono<Boolean> alreadyProcessed = key == null
                ? Mono.just(false)
                : idempotencyService.exists(key);

        return alreadyProcessed.flatMap(exists -> {
            if (exists) {
                return Mono.empty();
            }

            Mono<Void> persistKey = key == null
                    ? Mono.empty()
                    : idempotencyService.store(key, "OK");

            return service.addTransactions(jobId, txs).then(persistKey);
        });
    }

    /**
     * Fuehrt die Export-Pipeline fuer einen Job aus.
     *
     * @param jobId Job-ID
     * @return Ergebnis der Ausfuehrung
     */
    @PostMapping("/{jobId}/execute")
    public Mono<JobResult> execute(@PathVariable String jobId) {
        return service.execute(jobId);
    }

    /**
     * Liest einen Job inklusive aktuellem Status.
     *
     * @param jobId Job-ID
     * @return Jobdaten
     */
    @GetMapping("/{jobId}")
    public Mono<F15zJob> getJob(@PathVariable String jobId) {
        return service.getJob(jobId);
    }

    /**
     * Liefert das aggregierte Ergebnis fuer einen Job.
     *
     * @param jobId Job-ID
     * @return Ergebnisdaten
     */
    @GetMapping("/{jobId}/result")
    public Mono<JobResult> getResult(@PathVariable String jobId) {
        return service.getResult(jobId);
    }
}