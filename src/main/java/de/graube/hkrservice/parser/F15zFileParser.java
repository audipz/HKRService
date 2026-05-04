package de.graube.hkrservice.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser fuer F15Z-Exportdateien und pipe-separierte Rueckmeldungen.
 */
public class F15zFileParser {

    private static final int SPEC_RECORD_LENGTH = 570;

    /**
     * Repraesentiert einen Datensatz aus Datei- oder Rueckmeldedaten.
     */
    public static class Record {
        public String belegnummer;
        public BigDecimal amount;
        public String status;
        public String message;
    }

    /**
     * Enthaelt das komplette Parse-Ergebnis inklusive Trailer-Informationen.
     */
    public static class Parsed {
        public List<Record> records = new ArrayList<>();
        public List<BigDecimal> amounts = new ArrayList<>();
        public int count;
        public BigDecimal sum = BigDecimal.ZERO;
        public int trailerCount;
        public BigDecimal trailerSum = BigDecimal.ZERO;
        public boolean trailerIncludesEnvelope;
    }

    /**
     * Parst F15Z- oder Rueckmeldedateiinhalt zeilenweise.
     *
     * @param content Dateiinhalt
     * @return strukturiertes Parse-Ergebnis
     */
    public Parsed parse(String content) {
        Parsed parsed = new Parsed();

        for (String line : content.split("\\r?\\n")) {
            if (line.isBlank()) {
                continue;
            }

            if (line.startsWith("2|") || line.startsWith("R|")) {
                parseReturnRecord(parsed, line);
                continue;
            }

            if (line.startsWith("2") && line.length() >= SPEC_RECORD_LENGTH) {
                parseSpecRecord(parsed, line);
                continue;
            }

            if (line.startsWith("2")) {
                parseLegacyRecord(parsed, line);
                continue;
            }

            if (line.startsWith("9|")) {
                parsePipeTrailer(parsed, line);
                continue;
            }

            if (line.startsWith("9") && line.length() >= SPEC_RECORD_LENGTH) {
                parseSpecTrailer(parsed, line);
                continue;
            }

            if (line.startsWith("9")) {
                parseLegacyTrailer(parsed, line);
            }
        }

        return parsed;
    }

    private void parseLegacyRecord(Parsed parsed, String line) {
        if (line.length() < 36) {
            throw new IllegalArgumentException("Invalid F15z record line: " + line);
        }

        Record record = new Record();
        record.belegnummer = line.substring(5, 15).trim();
        record.amount = new BigDecimal(line.substring(23, 36).trim()).movePointLeft(2);

        parsed.records.add(record);
        parsed.amounts.add(record.amount);
    }

    private void parseLegacyTrailer(Parsed parsed, String line) {
        if (line.length() < 22) {
            throw new IllegalArgumentException("Invalid F15z trailer line: " + line);
        }

        parsed.count = Integer.parseInt(line.substring(1, 7).trim());
        parsed.sum = new BigDecimal(line.substring(7, 22).trim()).movePointLeft(2);
        parsed.trailerCount = parsed.count;
        parsed.trailerSum = parsed.sum;
        parsed.trailerIncludesEnvelope = false;
    }

    private void parseSpecRecord(Parsed parsed, String line) {
        // SK2 Feld 11 (Kassenzeichen): Position 78-89, Laenge 12
        // SK2 Feld 24 (Betrag in Cent): Position 260-272, Laenge 13
        if (line.length() < 273) {
            throw new IllegalArgumentException("Invalid F15z spec record line: " + line);
        }

        Record record = new Record();
        record.belegnummer = line.substring(78, 90).trim();
        record.amount = new BigDecimal(line.substring(260, 273).trim()).movePointLeft(2);

        parsed.records.add(record);
        parsed.amounts.add(record.amount);
    }

    private void parseSpecTrailer(Parsed parsed, String line) {
        // SK9 Feld 6 (Summe): Position 28-41, Laenge 14 (Cent)
        // SK9 Feld 7 (Anzahl): Position 42-46, Laenge 5 (inkl. SK1 + SK9)
        if (line.length() < 47) {
            throw new IllegalArgumentException("Invalid F15z spec trailer line: " + line);
        }

        parsed.count = Integer.parseInt(line.substring(42, 47).trim());
        parsed.sum = new BigDecimal(line.substring(28, 42).trim()).movePointLeft(2);
        parsed.trailerCount = parsed.count;
        parsed.trailerSum = parsed.sum;
        parsed.trailerIncludesEnvelope = true;
    }

    private void parsePipeTrailer(Parsed parsed, String line) {
        String[] parts = line.split("\\|", 3);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid trailer line: " + line);
        }

        parsed.count = Integer.parseInt(parts[1]);
        parsed.sum = new BigDecimal(parts[2]);
        parsed.trailerCount = parsed.count;
        parsed.trailerSum = parsed.sum;
        parsed.trailerIncludesEnvelope = false;
    }

    private void parseReturnRecord(Parsed parsed, String line) {
        String[] parts = line.split("\\|", 4);
        if (parts.length < 3) {
            throw new IllegalArgumentException("Invalid return line: " + line);
        }

        Record record = new Record();
        record.belegnummer = parts[1];
        record.status = parts[2];
        record.message = parts.length > 3 ? parts[3] : null;

        parsed.records.add(record);
    }
}