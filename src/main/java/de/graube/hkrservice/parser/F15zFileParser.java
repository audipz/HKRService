package de.graube.hkrservice.parser;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Parser fuer F15Z-Exportdateien und pipe-separierte Rueckmeldungen.
 */
public class F15zFileParser {

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
    }

    /**
     * Parst F15Z- oder Rueckmeldedateiinhalt zeilenweise.
     *
     * @param content Dateiinhalt
     * @return strukturiertes Parse-Ergebnis
     */
    public Parsed parse(String content) {
        Parsed parsed = new Parsed();

        for (String rawLine : content.split("\\r?\\n")) {
            String line = rawLine == null ? "" : rawLine;
            if (line.isBlank()) {
                continue;
            }

            if (line.startsWith("2|") || line.startsWith("R|")) {
                parseReturnRecord(parsed, line);
                continue;
            }

            if (line.startsWith("2")) {
                parseF15zRecord(parsed, line);
                continue;
            }

            if (line.startsWith("9|")) {
                parsePipeTrailer(parsed, line);
                continue;
            }

            if (line.startsWith("9")) {
                parseFixedWidthTrailer(parsed, line);
            }
        }

        return parsed;
    }

    private void parseF15zRecord(Parsed parsed, String line) {
        if (line.length() < 36) {
            throw new IllegalArgumentException("Invalid F15z record line: " + line);
        }

        Record record = new Record();
        record.belegnummer = line.substring(5, 15).trim();
        record.amount = new BigDecimal(line.substring(23, 36).trim()).movePointLeft(2);

        parsed.records.add(record);
        parsed.amounts.add(record.amount);
    }

    private void parseFixedWidthTrailer(Parsed parsed, String line) {
        if (line.length() < 22) {
            throw new IllegalArgumentException("Invalid F15z trailer line: " + line);
        }

        parsed.count = Integer.parseInt(line.substring(1, 7).trim());
        parsed.sum = new BigDecimal(line.substring(7, 22).trim()).movePointLeft(2);
        parsed.trailerCount = parsed.count;
        parsed.trailerSum = parsed.sum;
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