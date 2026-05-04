package de.graube.hkrservice.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Integrationstests fuer die fachlichen Buchungsarten.
 *
 * Wichtiger Hinweis zum Mapping auf das technische Enum VslType:
 * - FORDERUNG -> EINZ (Einzahlung/Forderungseingang)
 * - AUSZAHLUNG -> AUSZ
 * - RUECKZAHLUNG -> EINZ (Rueckfluss von Mitteln)
 * - UMBUCHUNG -> UMB
 * - HAUSHALTSMITTELRESERVIERUNG -> RES
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TransactionTypeIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUpClient() {
        // Der Client spricht immer gegen den zufaelligen Test-Port.
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    @Test
    void forderungFlowWorks() {
        // FORDERUNG wird als EINZ getestet.
        runSingleTransactionFlow(
                "EINZ",
                "BLG-FORD-" + UUID.randomUUID(),
                "FORDERUNG"
        );
    }

    @Test
    void auszahlungFlowWorks() {
        // AUSZAHLUNG wird als AUSZ getestet.
        runSingleTransactionFlow(
                "AUSZ",
                "BLG-AUSZ-" + UUID.randomUUID(),
                "AUSZAHLUNG"
        );
    }

    @Test
    void rueckzahlungFlowWorks() {
        // RUECKZAHLUNG wird als EINZ getestet (Mittelrueckfluss).
        runSingleTransactionFlow(
                "EINZ",
                "BLG-RUECK-" + UUID.randomUUID(),
                "RUECKZAHLUNG"
        );
    }

    @Test
    void umbuchungFlowWorks() {
        // UMBUCHUNG wird als UMB getestet.
        runSingleTransactionFlow(
                "UMB",
                "BLG-UMB-" + UUID.randomUUID(),
                "UMBUCHUNG"
        );
    }

    @Test
    void haushaltsmittelreservierungFlowWorks() {
        // HAUSHALTSMITTELRESERVIERUNG wird als RES getestet.
        runSingleTransactionFlow(
                "RES",
                "BLG-RES-" + UUID.randomUUID(),
                "HAUSHALTSMITTELRESERVIERUNG"
        );
    }

    private void runSingleTransactionFlow(String type, String belegnummer, String fachbegriff) {
        // 1) Job erzeugen.
        String jobId = createJob();

        // 2) Genau eine Transaktion der gewuenschten Buchungsart einstellen.
        webTestClient.post()
                .uri("/jobs/{jobId}/transactions", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "type-" + fachbegriff + "-" + UUID.randomUUID())
                .bodyValue(buildTransactionPayload(type, belegnummer, fachbegriff))
                .exchange()
                .expectStatus().isOk();

        // 3) Verarbeitung starten.
        webTestClient.post()
                .uri("/jobs/{jobId}/execute", jobId)
                .exchange()
                .expectStatus().isOk()
                // Bei einer gueltigen Einzeltransaktion erwarten wir genau einen Erfolg.
                .expectBody()
                .jsonPath("$.status").isEqualTo("SENT")
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.success").isEqualTo(1)
                .jsonPath("$.error").isEqualTo(0);

        // 4) Ergebnis abrufen und erneut validieren.
        webTestClient.get()
                .uri("/jobs/{jobId}/result", jobId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SENT")
                .jsonPath("$.total").isEqualTo(1)
                .jsonPath("$.success").isEqualTo(1)
                .jsonPath("$.error").isEqualTo(0);
    }

    private String createJob() {
        // Job anlegen und jobId aus dem JSON extrahieren.
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

    private String buildTransactionPayload(String type, String belegnummer, String fachbegriff) {
        // Einheitliches Payload-Template fuer alle Buchungsarten.
        return """
                [{
                  "type":"%s",
                  "belegnummer":"%s",
                  "faelligkeit":"2026-05-01",
                  "betrag":100.00,
                  "iban":"DE89370400440532013000",
                  "bic":"COBADEFFXXX",
                  "titel":"%s",
                  "objektkonto":"OBJ-1"
                }]
                """.formatted(type, belegnummer, fachbegriff);
    }
}

