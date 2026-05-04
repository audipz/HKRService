# HKRService (R2DBC Version)

Diese Version nutzt eine komplett reaktive Pipeline mit:

- Spring WebFlux (Netty)
- Spring Data R2DBC
- H2 (in-memory) via R2DBC
- Optimistic Locking auf `f15z_jobs.version`
- Echten DB-Constraints (FK + UNIQUE + NOT NULL)

## Schnellstart

```bash
mvn clean test
mvn spring-boot:run
```

## Tests

- Unit-Tests:
  - `de.graube.hkrservice.parser.F15zFileParserTest`
  - `de.graube.hkrservice.validation.F15zValidatorTest`
- Integrationstests:
  - `de.graube.hkrservice.integration.JobFlowIntegrationTest`
    - enthält auch einen Parallelitäts-Execute-Test
  - `de.graube.hkrservice.integration.TransactionTypeIntegrationTest`
    - FORDERUNG, AUSZAHLUNG, RUECKZAHLUNG, UMBUCHUNG, HAUSHALTSMITTELRESERVIERUNG
    - enthält explizite Tests plus parametrisierte Variante (`@ParameterizedTest`)
  - `de.graube.hkrservice.repositorys.R2dbcRepositoryIntegrationTest`

## Endpunkt testen

```bash
curl -s -X POST http://localhost:8080/jobs
```

```bash
curl -s -X POST http://localhost:8080/jobs/<JOB_ID>/transactions \
  -H "Content-Type: application/json" \
  -H "Idempotency-Key: demo-key-1" \
  -d '[
    {
      "type": "AUSZ",
      "belegnummer": "BLG1001",
      "faelligkeit": "2026-05-01",
      "betrag": 123.45,
      "iban": "DE89370400440532013000",
      "bic": "COBADEFFXXX",
      "titel": "MIETE",
      "objektkonto": "OBJ-1"
    }
  ]'
```

```bash
curl -s -X POST http://localhost:8080/jobs/<JOB_ID>/execute
curl -s http://localhost:8080/jobs/<JOB_ID>/result
```

## Fachliche Dokumentation

### Ziel und fachlicher Kontext

Der Service verarbeitet F15Z-Zahlungsdaten in einem klaren Job-Lebenszyklus.

- Ein **Job** bildet einen fachlichen Verarbeitungslauf.
- Ein Job enthaelt **Transaktionen** (z. B. AUSZ, EINZ, UMB, RES).
- Der Job wird als F15Z-Datei exportiert und anschliessend auf Rueckmeldungen verarbeitet.

### Fachobjekte

- `F15zJob`: fachliche Klammer fuer Erfassung, Versand und Rueckmeldung.
- `F15zTransaction` (Model + Entity): einzelne Zahlungsposition mit Betrag, Faelligkeit, IBAN/BIC und Belegnummer.
- `JobResult`: aggregiertes Ergebnis pro Job (gesamt/erfolgreich/fehlerhaft).
- `IdempotencyRecord`: verhindert Doppelverarbeitung bei wiederholten API-Aufrufen.

### Prozessablauf

1. **Job anlegen**
   - `POST /jobs`
   - Ergebnis: neuer Job mit Status `CREATED`.
2. **Transaktionen sammeln**
   - `POST /jobs/{jobId}/transactions`
   - Job wechselt nach `COLLECTING`.
   - Optional mit `Idempotency-Key` fuer technisch idempotente Verarbeitung.
3. **Ausfuehren / Exportieren**
   - `POST /jobs/{jobId}/execute`
   - Generator erstellt F15Z-Daten, Parser + Validator pruefen Format und Trailer.
   - Bei Erfolg: Statusverlauf `PROCESSING -> EXPORTED -> SENT`.
   - Bei Fehler: Status `FAILED` inkl. Fehlermeldung im Ergebnis.
4. **Rueckmeldungen verarbeiten**
   - `POST /returns/{jobId}`
   - Pro Rueckmeldezeile wird die Transaktion auf `CONFIRMED` oder `ERROR` gesetzt.
   - Sind alle Transaktionen terminal, wechselt der Job auf `COMPLETED`.

### Statusmodell

#### Jobstatus (`JobStatus`)

- `CREATED`: Job angelegt, noch keine oder erste Daten.
- `COLLECTING`: Transaktionen werden gesammelt.
- `PROCESSING`: exklusive Execute-Ausfuehrung aktiv.
- `EXPORTED`: F15Z-Export intern erstellt.
- `SENT`: fachlich versendet.
- `COMPLETED`: Rueckmeldungen komplett verarbeitet.
- `FAILED`: Verarbeitung mit Fehler beendet.

#### Transaktionsstatus (`TransaktionStatus`)

- `NEW`: frisch erfasst, noch ohne Rueckmeldung.
- `CONFIRMED`: Rueckmeldung erfolgreich.
- `ERROR`: Rueckmeldung mit fachlichem Fehler.

### Fachregeln und Plausibilitaeten

- Ein Execute ist nur fuer Jobs im Status `COLLECTING` zulaessig.
- Doppelte oder parallele Execute-Aufrufe werden konfliktbehaftet abgewiesen.
- F15Z-Trailer wird gegen berechnete Anzahl und Summe validiert.
- Rueckmeldungen duerfen nur fuer Jobs im Status `SENT` verarbeitet werden.
- Terminale Transaktionen (`CONFIRMED`/`ERROR`) werden nicht erneut ueberschrieben.

### Idempotenz und Parallelitaet

- `Idempotency-Key` auf `POST /jobs/{jobId}/transactions` verhindert doppelte technische Verarbeitung.
- Optimistic Locking auf `f15z_jobs.version` schuetzt vor parallelen Statusrennen.
- Konflikte werden als HTTP `409 CONFLICT` abgebildet.

### Fehlerabbildung nach API

Der globale Exception-Handler mappt typische Fach- und Laufzeitfehler auf HTTP-Status:

- `400 BAD_REQUEST`: ungueltige Eingabedaten.
- `404 NOT_FOUND`: Job nicht gefunden.
- `409 CONFLICT`: Statuskonflikte, Parallelitaet, Constraint-Verletzungen.
- `500 INTERNAL_SERVER_ERROR`: unerwartete technische Fehler.

### Architekturueberblick

#### Komponentenbild

```text
Client
  |
  v
Controller (`F15zController`, `ReturnController`)
  |
  v
Service-Schicht (`F15zService`, `F15zReturnService`, `IdempotencyService`)
  |                    |
  |                    +--> Formatlogik (`F15zGenerator`, `F15zFileParser`, `F15zValidator`)
  |
  +--> Repositories (`JobRepository`, `F15zTransactionRepository`, `IdempotencyRepository`)
         |
         v
       R2DBC + H2
```

#### Sequenzfluss (Happy Path)

```text
1) Client -> POST /jobs
2) Controller -> Service.createJob()
3) Service -> JobRepository.save(CREATED)

4) Client -> POST /jobs/{jobId}/transactions
5) Controller -> IdempotencyService.exists/store (optional)
6) Service -> JobRepository.save(COLLECTING)
7) Service -> F15zTransactionRepository.save(...)

8) Client -> POST /jobs/{jobId}/execute
9) Service -> claimForExecution(PROCESSING)
10) Service -> F15zGenerator -> F15zFileParser -> F15zValidator
11) Service -> Jobstatus EXPORTED -> SENT
12) Client -> GET /jobs/{jobId}/result

13) Client -> POST /returns/{jobId}
14) ReturnService -> Parser Rueckmeldedatei
15) ReturnService -> Transaktionen auf CONFIRMED/ERROR
16) ReturnService -> bei vollstaendig terminal: Jobstatus COMPLETED
```

#### Technische Leitplanken

- Reaktive Verarbeitung via `Mono`/`Flux` fuer API und Persistenzpfad.
- Statusuebergaenge werden fachlich im Service erzwungen.
- Optimistic Locking schuetzt kritische Job-Statuswechsel.
- Idempotenz ist fuer das Hinzufuegen von Transaktionen explizit unterstuetzt.

