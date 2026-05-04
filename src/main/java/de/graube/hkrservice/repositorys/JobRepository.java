package de.graube.hkrservice.repositorys;

import de.graube.hkrservice.entity.F15zJob;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

/**
 * Reaktives Repository fuer F15Z-Jobs.
 */
public interface JobRepository extends ReactiveCrudRepository<F15zJob, String> {
}
