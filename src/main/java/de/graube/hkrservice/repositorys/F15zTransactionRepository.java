package de.graube.hkrservice.repositorys;

import de.graube.hkrservice.entity.F15zTransaction;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface F15zTransactionRepository extends ReactiveCrudRepository<F15zTransaction, String> {

    Flux<F15zTransaction> findByJobId(String jobId);
}

