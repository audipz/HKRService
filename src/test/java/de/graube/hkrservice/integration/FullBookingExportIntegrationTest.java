package de.graube.hkrservice.integration;

import de.graube.hkrservice.dto.JobStatus;
import de.graube.hkrservice.entity.F15zJob;
import de.graube.hkrservice.generator.F15zGenerator;
import de.graube.hkrservice.model.F15zTransaction;
import de.graube.hkrservice.model.VslType;
import de.graube.hkrservice.parser.F15zFileParser;
import de.graube.hkrservice.pdf.SammelanordnungPdfBox;
import de.graube.hkrservice.repositorys.F15zTransactionRepository;
import de.graube.hkrservice.repositorys.JobRepository;
import de.graube.hkrservice.validation.F15zValidator;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Umfassender Integrationstest:
 *
 * <ol>
 *   <li>Verschiedene Buchungsarten (AUSZ, EINZ, UMB, RES) einstellen</li>
 *   <li>DB-Zustand validieren (Status, Typen, Beträge)</li>
 *   <li>F15z-Exportdatei generieren und Dateiinhalt gegen DB prüfen</li>
 *   <li>F15z-Validator über die generierte Datei laufen lassen</li>
 *   <li>Execute-Endpoint aufrufen und Ergebnis prüfen</li>
 *   <li>Sammelanordnungs-PDF erstellen und inhaltlich validieren</li>
 * </ol>
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FullBookingExportIntegrationTest {

    // =========================================================================
    // Felder
    // =========================================================================

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @Autowired
    private JobRepository jobRepository;

    @Autowired
    private F15zTransactionRepository transactionRepository;

    // =========================================================================
    // Setup
    // =========================================================================

    @BeforeEach
    void setUp() {
        this.webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .build();
    }

    // =========================================================================
    // Test
    // =========================================================================

    /**
     * Haupttest: Buchungen anlegen → DB validieren → Datei prüfen → Execute →
     * PDF erstellen.
     *
     * <p>Belegnummern sind auf max. 10 Zeichen begrenzt, damit keine Trunkierung
     * in der Exportdatei entsteht und der Vergleich eindeutig ist.</p>
     */
    @Test
    void multipleBuchungenExportDateiUndPdfStimmenMitDbUeberein() throws Exception {

        // ------------------------------------------------------------------
        // 1) Job anlegen
        // ------------------------------------------------------------------
        String jobId = createJob();

        // ------------------------------------------------------------------
        // Testdaten (Belegnummern ≤ 10 Zeichen)
        // ------------------------------------------------------------------
        final String blgAusz = "TEST-AUSZ";
        final String blgEinz = "TEST-EINZ";
        final String blgUmb  = "TEST-UMB";
        final String blgRes  = "TEST-RES";

        final BigDecimal betragAusz = new BigDecimal("150.00");
        final BigDecimal betragEinz = new BigDecimal("200.50");
        final BigDecimal betragUmb  = new BigDecimal("75.25");
        final BigDecimal betragRes  = new BigDecimal("300.00");

        final BigDecimal expectedSum = betragAusz
                .add(betragEinz)
                .add(betragUmb)
                .add(betragRes);   // = 725.75

        // ------------------------------------------------------------------
        // 2) Buchungen über den REST-Endpunkt einstellen
        // ------------------------------------------------------------------
        addTransaction(jobId, "AUSZ", blgAusz, betragAusz, "Auszahlung");
        addTransaction(jobId, "EINZ", blgEinz, betragEinz, "Forderung");
        addTransaction(jobId, "UMB",  blgUmb,  betragUmb,  "Umbuchung");
        addTransaction(jobId, "RES",  blgRes,  betragRes,  "Reservierung");

        // ------------------------------------------------------------------
        // 3) DB-Zustand validieren
        // ------------------------------------------------------------------

        // 3a) Job muss im Status COLLECTING sein
        F15zJob job = jobRepository.findById(jobId).block();
        assertNotNull(job, "Job muss in der DB vorhanden sein");
        assertEquals(JobStatus.COLLECTING, job.getStatus(),
                "Job-Status muss nach dem Einstellen der Buchungen COLLECTING sein");

        // 3b) Genau 4 Transaktionen vorhanden
        List<de.graube.hkrservice.entity.F15zTransaction> dbTxs =
                transactionRepository.findByJobId(jobId).collectList().block();
        assertNotNull(dbTxs, "Transaktionsliste darf nicht null sein");
        assertEquals(4, dbTxs.size(),
                "Genau 4 Transaktionen müssen in der DB gespeichert sein");

        // 3c) Typen korrekt gespeichert
        Map<String, String> expectedTypen = Map.of(
                blgAusz, "AUSZ",
                blgEinz, "EINZ",
                blgUmb,  "UMB",
                blgRes,  "RES"
        );
        for (de.graube.hkrservice.entity.F15zTransaction dbTx : dbTxs) {
            String erwarteterTyp = expectedTypen.get(dbTx.getBelegnummer());
            assertNotNull(erwarteterTyp,
                    "Unbekannte Belegnummer in DB: " + dbTx.getBelegnummer());
            assertEquals(erwarteterTyp, dbTx.getType(),
                    "Falscher Typ für Belegnummer " + dbTx.getBelegnummer());
        }

        // 3d) Beträge korrekt gespeichert
        Map<String, BigDecimal> expectedBetraege = Map.of(
                blgAusz, betragAusz,
                blgEinz, betragEinz,
                blgUmb,  betragUmb,
                blgRes,  betragRes
        );
        for (de.graube.hkrservice.entity.F15zTransaction dbTx : dbTxs) {
            BigDecimal erwartetBetrag = expectedBetraege.get(dbTx.getBelegnummer());
            assertNotNull(erwartetBetrag);
            assertEquals(0, erwartetBetrag.compareTo(dbTx.getBetrag()),
                    "Falsche Betrag für Belegnummer " + dbTx.getBelegnummer()
                            + ": erwartet=" + erwartetBetrag + ", tatsächlich=" + dbTx.getBetrag());
        }

        // 3e) Alle Belegnummern in DB vorhanden
        Set<String> dbBelegnummern = dbTxs.stream()
                .map(de.graube.hkrservice.entity.F15zTransaction::getBelegnummer)
                .collect(Collectors.toSet());
        assertTrue(dbBelegnummern.containsAll(Set.of(blgAusz, blgEinz, blgUmb, blgRes)),
                "Nicht alle Belegnummern in der DB vorhanden. DB enthält: " + dbBelegnummern);

        // ------------------------------------------------------------------
        // 4) F15z-Exportdatei aus DB-Transaktionen generieren
        // ------------------------------------------------------------------
        List<F15zTransaction> modelTxs = dbTxs.stream()
                .map(this::toModel)
                .toList();

        String fileContent = new F15zGenerator().build(modelTxs);

        assertNotNull(fileContent, "Generierte Exportdatei darf nicht null sein");
        assertFalse(fileContent.isBlank(), "Generierte Exportdatei darf nicht leer sein");

        // 4a) Exportdatei parsen
        F15zFileParser.Parsed parsed = new F15zFileParser().parse(fileContent);

        // 4b) Anzahl der Datensätze stimmt
        assertEquals(4, parsed.records.size(),
                "Exportdatei muss genau 4 Datensätze (Satzart 2) enthalten");

        // 4c) Trailer-Anzahl stimmt mit DB überein
        assertEquals(4, parsed.trailerCount,
                "Trailer-Anzahl muss 4 betragen");

        // 4d) Gesamtsumme im Trailer stimmt mit DB-Summe überein
        assertEquals(0, expectedSum.compareTo(parsed.trailerSum),
                "Gesamtsumme im Trailer stimmt nicht. Erwartet: "
                        + expectedSum + ", tatsächlich: " + parsed.trailerSum);

        // 4e) Summe der Einzelbeträge in der Datei stimmt ebenfalls
        BigDecimal parsedTotal = parsed.amounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, expectedSum.compareTo(parsedTotal),
                "Summe der Einzelbeträge in der Exportdatei stimmt nicht. Erwartet: "
                        + expectedSum + ", tatsächlich: " + parsedTotal);

        // 4f) Belegnummern in Datei vorhanden
        Set<String> parsedBelegnummern = parsed.records.stream()
                .map(r -> r.belegnummer)
                .collect(Collectors.toSet());
        assertTrue(parsedBelegnummern.contains(blgAusz),
                "Belegnummer '" + blgAusz + "' fehlt in der Exportdatei");
        assertTrue(parsedBelegnummern.contains(blgEinz),
                "Belegnummer '" + blgEinz + "' fehlt in der Exportdatei");
        assertTrue(parsedBelegnummern.contains(blgUmb),
                "Belegnummer '" + blgUmb + "' fehlt in der Exportdatei");
        assertTrue(parsedBelegnummern.contains(blgRes),
                "Belegnummer '" + blgRes + "' fehlt in der Exportdatei");

        // 4g) Beträge pro Belegnummer in der Datei stimmen mit DB überein
        Map<String, BigDecimal> parsedBetragMap = new HashMap<>();
        for (F15zFileParser.Record rec : parsed.records) {
            parsedBetragMap.put(rec.belegnummer, rec.amount);
        }
        assertEquals(0, betragAusz.compareTo(parsedBetragMap.get(blgAusz)),
                "Betrag für AUSZ in Exportdatei stimmt nicht");
        assertEquals(0, betragEinz.compareTo(parsedBetragMap.get(blgEinz)),
                "Betrag für EINZ in Exportdatei stimmt nicht");
        assertEquals(0, betragUmb.compareTo(parsedBetragMap.get(blgUmb)),
                "Betrag für UMB in Exportdatei stimmt nicht");
        assertEquals(0, betragRes.compareTo(parsedBetragMap.get(blgRes)),
                "Betrag für RES in Exportdatei stimmt nicht");

        // 4h) F15z-Validator läuft ohne Fehler (Count + Sum-Check)
        assertDoesNotThrow(() -> new F15zValidator().validate(parsed),
                "F15z-Validierung der Exportdatei darf keinen Fehler werfen");

        // ------------------------------------------------------------------
        // 5) Job ausführen (Execute-Endpoint)
        // ------------------------------------------------------------------
        webTestClient.post()
                .uri("/jobs/{jobId}/execute", jobId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SENT")
                .jsonPath("$.total").isEqualTo(4)
                .jsonPath("$.success").isEqualTo(4)
                .jsonPath("$.error").isEqualTo(0);

        // Job-Status nach Execute: SENT
        webTestClient.get()
                .uri("/jobs/{jobId}", jobId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SENT");

        // Ergebnis-Endpunkt
        webTestClient.get()
                .uri("/jobs/{jobId}/result", jobId)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("SENT")
                .jsonPath("$.total").isEqualTo(4)
                .jsonPath("$.success").isEqualTo(4)
                .jsonPath("$.error").isEqualTo(0);

        // ------------------------------------------------------------------
        // 6) Sammelanordnungs-PDF erstellen und validieren
        // ------------------------------------------------------------------
        Path pdfPath = Files.createTempFile("sammelanordnung-it-", ".pdf");
        try {
            // PDF generieren
            assertDoesNotThrow(
                    () -> new SammelanordnungPdfBox().generate(pdfPath.toString(), modelTxs),
                    "PDF-Generierung darf keinen Fehler werfen"
            );

            // Datei muss existieren und nicht leer sein
            assertTrue(Files.exists(pdfPath), "PDF-Datei muss auf dem Dateisystem existieren");
            assertTrue(Files.size(pdfPath) > 0, "PDF-Datei darf nicht leer sein");

            // PDF inhaltlich prüfen
            try (PDDocument doc = PDDocument.load(pdfPath.toFile())) {
                assertFalse(doc.isEncrypted(), "PDF darf nicht verschlüsselt sein");
                assertTrue(doc.getNumberOfPages() >= 1,
                        "PDF muss mindestens eine Seite enthalten");

                String pdfText = new PDFTextStripper().getText(doc);
                assertFalse(pdfText.isBlank(), "PDF-Text nach Extraktion darf nicht leer sein");

                // Anzahl der Buchungen im PDF-Text
                assertTrue(pdfText.contains("4"),
                        "PDF-Text muss die Buchungsanzahl '4' enthalten. "
                                + "Tatsächlicher Text: " + pdfText);

                // Bezeichnung im PDF-Text
                assertTrue(
                        pdfText.toLowerCase(Locale.ROOT).contains("sammelanordnung"),
                        "PDF-Text muss das Wort 'Sammelanordnung' enthalten. "
                                + "Tatsächlicher Text: " + pdfText
                );
            }
        } finally {
            Files.deleteIfExists(pdfPath);
        }
    }

    // =========================================================================
    // Hilfsmethoden
    // =========================================================================

    /** Legt einen neuen Job an und gibt die jobId zurück. */
    private String createJob() {
        String body = webTestClient.post()
                .uri("/jobs")
                .exchange()
                .expectStatus().isOk()
                .expectBody(String.class)
                .returnResult()
                .getResponseBody();

        Matcher matcher = Pattern.compile("\"jobId\":\"([^\"]+)\"")
                .matcher(body == null ? "" : body);
        if (!matcher.find()) {
            throw new IllegalStateException("jobId fehlt in der Antwort: " + body);
        }
        return matcher.group(1);
    }

    /**
     * Fügt dem Job eine einzelne Transaktion hinzu.
     *
     * @param jobId       Ziel-Job
     * @param type        VslType-Wert (AUSZ, EINZ, UMB, RES)
     * @param belegnummer Eindeutige Belegnummer (≤ 10 Zeichen empfohlen)
     * @param betrag      Buchungsbetrag
     * @param titel       Freitextfeld
     */
    private void addTransaction(String jobId,
                                String type,
                                String belegnummer,
                                BigDecimal betrag,
                                String titel) {
        webTestClient.post()
                .uri("/jobs/{jobId}/transactions", jobId)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Idempotency-Key", "export-it-" + UUID.randomUUID())
                .bodyValue("""
                        [{
                          "type":"%s",
                          "belegnummer":"%s",
                          "faelligkeit":"2026-05-01",
                          "betrag":%s,
                          "iban":"DE89370400440532013000",
                          "bic":"COBADEFFXXX",
                          "titel":"%s",
                          "objektkonto":"OBJ-IT"
                        }]
                        """.formatted(type, belegnummer, betrag.toPlainString(), titel))
                .exchange()
                .expectStatus().isOk();
    }

    /** Konvertiert eine DB-Entity in das Modell für Generator/PDF. */
    private F15zTransaction toModel(de.graube.hkrservice.entity.F15zTransaction db) {
        F15zTransaction tx = new F15zTransaction();
        tx.setType(VslType.valueOf(db.getType()));
        tx.setBelegnummer(db.getBelegnummer());
        tx.setFaelligkeit(db.getFaelligkeit());
        tx.setBetrag(db.getBetrag());
        tx.setIban(db.getIban());
        tx.setBic(db.getBic());
        tx.setTitel(db.getTitel());
        tx.setObjektkonto(db.getObjektkonto());
        return tx;
    }
}

