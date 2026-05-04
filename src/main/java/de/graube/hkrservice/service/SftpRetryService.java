package de.graube.hkrservice.service;

import de.graube.hkrservice.sftp.SftpClient;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;

import java.time.Duration;

/**
 * Fuehrt SFTP-Uploads mit Retry-Strategie aus.
 */
@Service
public class SftpRetryService {

    private final SftpClient client = new SftpClient();

    /**
     * Sendet Inhalte ueber SFTP mit exponentiellem Backoff.
     *
     * @param content Nutzinhalt
     * @return Abschlusssignal
     */
    public Mono<Void> send(String content) {
        return Mono.fromRunnable(() -> {
                    try {
                        client.upload(content);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                })
                .subscribeOn(Schedulers.boundedElastic())
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)))
                .then();
    }
}