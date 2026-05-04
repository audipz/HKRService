package de.graube.hkrservice.repositorys;

import de.graube.hkrservice.entity.F15zTransaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Reaktives Repository fuer F15Z-Transaktionen.
 */
public interface F15zTransactionRepository extends ReactiveCrudRepository<F15zTransaction, String> {

    /**
     * Liefert alle Transaktionen zu einem Job.
     */
    Flux<F15zTransaction> findByJobId(String jobId);

    /**
     * Sucht eine Transaktion ueber Job-ID und Belegnummer.
     */
    Mono<F15zTransaction> findByJobIdAndBelegnummer(String jobId, String belegnummer);
}

