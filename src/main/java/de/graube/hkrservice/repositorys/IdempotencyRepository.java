package de.graube.hkrservice.repositorys;

import de.graube.hkrservice.entity.IdempotencyRecord;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface IdempotencyRepository extends ReactiveCrudRepository<IdempotencyRecord, String> {
}

