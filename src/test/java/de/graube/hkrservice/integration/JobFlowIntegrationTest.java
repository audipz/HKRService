package de.graube.hkrservice.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class JobFlowIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUpClient() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void fullFlowWithConstraintAndReturnCompletesJob() throws Exception {
        String jobId = createJob();

        webTestClient.post()
                .uri("/jobs/{jobId}/transactions", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "it-key-" + UUID.randomUUID())
                .bodyValue("""
                        [{
                          "type":"AUSZ",
                          "belegnummer":"BLG-IT-1",
                          "faelligkeit":"2026-05-01",
                          "betrag":100.00,
                          "iban":"DE89370400440532013000",
                          "bic":"COBADEFFXXX",
                          "titel":"T1",
                          "objektkonto":"O1"
                        }]
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        webTestClient.post()
                .uri("/jobs/{jobId}/transactions", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "it-key-" + UUID.randomUUID())
                .bodyValue("""
                        [{
                          "type":"AUSZ",
                          "belegnummer":"BLG-IT-1",
                          "faelligkeit":"2026-05-01",
                          "betrag":100.00,
                          "iban":"DE89370400440532013000",
                          "bic":"COBADEFFXXX",
                          "titel":"T1",
                          "objektkonto":"O1"
                        }]
                        """)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.detail").value(v -> ((String) v).contains("Constraint verletzt"));

        webTestClient.post()
                .uri("/jobs/{jobId}/execute", jobId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SENT")
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.success").isEqualTo(1);

        webTestClient.post()
                .uri("/returns/{jobId}", jobId)
                .contentType(MediaType.TEXT_PLAIN)
                .bodyValue("2|BLG-IT-1|OK")
                .exchange()
                .expectStatus().isOk()
                .expectBody().isEmpty();

        webTestClient.get()
                .uri("/jobs/{jobId}", jobId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("COMPLETED");

        webTestClient.get()
                .uri("/jobs/{jobId}/result", jobId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("COMPLETED")
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.success").isEqualTo(1);
    }

    @Test
    void sameIdempotencyKeySkipsSecondTransactionPayload() throws Exception {
        String jobId = createJob();
        String sameKey = "idem-" + UUID.randomUUID();

        webTestClient.post()
                .uri("/jobs/{jobId}/transactions", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", sameKey)
                .bodyValue("""
                        [{
                          "type":"AUSZ",
                          "belegnummer":"BLG-IDEM-1",
                          "faelligkeit":"2026-05-01",
                          "betrag":100.00,
                          "iban":"DE89370400440532013000",
                          "bic":"COBADEFFXXX",
                          "titel":"T1",
                          "objektkonto":"O1"
                        }]
                        """)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/jobs/{jobId}/transactions", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", sameKey)
                .bodyValue("""
                        [{
                          "type":"AUSZ",
                          "belegnummer":"BLG-IDEM-2",
                          "faelligkeit":"2026-05-01",
                          "betrag":200.00,
                          "iban":"DE89370400440532013000",
                          "bic":"COBADEFFXXX",
                          "titel":"T2",
                          "objektkonto":"O2"
                        }]
                        """)
                .exchange()
                .expectStatus().isOk();

        webTestClient.post()
                .uri("/jobs/{jobId}/execute", jobId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SENT")
                .jsonPath("$.total").isEqualTo(1);
    }

    @Test
    void unknownJobResultReturns404() {
        webTestClient.get()
                .uri("/jobs/{jobId}/result", "does-not-exist")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Job not found");
    }

    @Test
    void parallelExecuteProducesAtLeastOneFailedResponse() throws Exception {
        String jobId = createJob();

        webTestClient.post()
                .uri("/jobs/{jobId}/transactions", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "parallel-" + UUID.randomUUID())
                .bodyValue("""
                        [{
                          "type":"AUSZ",
                          "belegnummer":"BLG-PAR-1",
                          "faelligkeit":"2026-05-01",
                          "betrag":100.00,
                          "iban":"DE89370400440532013000",
                          "bic":"COBADEFFXXX",
                          "titel":"T1",
                          "objektkonto":"O1"
                        }]
                        """)
                .exchange()
                .expectStatus().isOk();

        CyclicBarrier barrier = new CyclicBarrier(2);

        CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> executeWithBarrier(jobId, barrier));
        CompletableFuture<String> second = CompletableFuture.supplyAsync(() -> executeWithBarrier(jobId, barrier));

        List<String> bodies = List.of(
                first.get(10, TimeUnit.SECONDS),
                second.get(10, TimeUnit.SECONDS)
        );

        long failedCount = bodies.stream()
                .map(body -> extractJsonField(body, "status"))
                .filter("FAILED"::equals)
                .count();

        assertTrue(failedCount >= 1, "Erwartet mindestens ein FAILED-Ergebnis bei Parallel-Execute");

        webTestClient.get()
                .uri("/jobs/{jobId}", jobId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").value(value -> {
                    String status = String.valueOf(value);
                    assertTrue("SENT".equals(status) || "FAILED".equals(status),
                            "Jobstatus nach Parallel-Execute sollte SENT oder FAILED sein");
                });
    }

    private String createJob() throws Exception {
        String body = webTestClient.post()
                .uri("/jobs")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Matcher matcher = Pattern.compile("\\\"jobId\\\":\\\"([^\\\"]+)\\\"").matcher(body == null ? "" : body);
        if (!matcher.find()) {
            throw new IllegalStateException("jobId fehlt in Antwort: " + body);
        }
        return matcher.group(1);
    }

    private String executeWithBarrier(String jobId, CyclicBarrier barrier) {
        try {
            barrier.await(10, TimeUnit.SECONDS);
            return webTestClient.post()
                    .uri("/jobs/{jobId}/execute", jobId)
                    .exchange()
                    .expectStatus().isOk()
                    .expectBody(String.class)
                    .returnResult()
                    .getResponseBody();
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private String extractJsonField(String body, String fieldName) {
        Matcher matcher = Pattern.compile("\\\"" + fieldName + "\\\":\\\"([^\\\"]+)\\\"")
                .matcher(body == null ? "" : body);
        if (!matcher.find()) {
            throw new IllegalStateException(fieldName + " fehlt in Antwort: " + body);
        }
        return matcher.group(1);
    }
}

