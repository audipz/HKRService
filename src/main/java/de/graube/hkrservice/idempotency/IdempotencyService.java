package de.graube.hkrservice.idempotency;

import de.graube.hkrservice.entity.IdempotencyRecord;
import de.graube.hkrservice.repositorys.IdempotencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

/**
 * Kapselt Speicherung und Abfrage von Idempotenz-Schluesseln.
 */
@Component
@RequiredArgsConstructor
public class IdempotencyService {

    private final IdempotencyRepository repository;

    /**
     * Speichert einen Idempotenz-Key und ignoriert Duplikate.
     *
     * @param key Idempotenz-Key
     * @param result gespeichertes Ergebnis
     * @return Abschlusssignal
     */
    public Mono<Void> store(String key, String result) {
        IdempotencyRecord record = new IdempotencyRecord();
        record.setIdempotencyKey(key);
        record.setResult(result);
        record.setCreatedAt(LocalDateTime.now());
        record.setNewEntity(true);

        return repository.save(record)
                .onErrorResume(DuplicateKeyException.class, ex -> Mono.empty())
                .then();
    }

    /**
     * Liest das gespeicherte Ergebnis zu einem Idempotenz-Key.
     */
    public Mono<String> get(String key) {
        return repository.findById(key)
                .map(IdempotencyRecord::getResult);
    }

    /**
     * Prueft, ob ein Idempotenz-Key bereits existiert.
     */
    public Mono<Boolean> exists(String key) {
        return repository.existsById(key);
    }
}