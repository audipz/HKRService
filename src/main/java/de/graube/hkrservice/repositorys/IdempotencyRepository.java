package de.graube.hkrservice.repositorys;

import de.graube.hkrservice.entity.IdempotencyRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Reaktives Repository fuer gespeicherte Idempotenz-Keys.
 */
public interface IdempotencyRepository extends ReactiveCrudRepository<IdempotencyRecord, String> {
}

