package de.graube.hkrservice.generator;

import de.graube.hkrservice.model.F15zTransaction;
import de.graube.hkrservice.util.Fixed200;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Erzeugt F15Z-Dateiinhalte im Festlaengenformat.
 */
public class F15zGenerator {

    private static final int RECORD_LENGTH = 570;
    private static final String DATEI_KENNZEICHEN = "HKR00001";
    private static final String BEWIRTSCHAFTER = "03000001";

    /**
     * Baut eine komplette F15Z-Datei aus einer Transaktionsliste.
     *
     * @param txs Transaktionen
     * @return Dateiinhalt
     */
    public String build(List<F15zTransaction> txs) {

        StringBuilder sb = new StringBuilder();

        sb.append(header());

        BigDecimal sum = BigDecimal.ZERO;
        int laufendeNummer = 1;
        long bicChecksum = 0;
        long ibanChecksum = 0;

        for (F15zTransaction tx : txs) {
            sb.append(satz2(tx, laufendeNummer++)).append("\n");
            sum = sum.add(tx.getBetrag() == null ? BigDecimal.ZERO : tx.getBetrag());
            bicChecksum += calculateBicChecksum(tx.getBic());
            ibanChecksum += calculateIbanChecksum(tx.getIban());
        }

        // SK9 Feld 7 zaehlt Datensaetze inkl. SK1 + SK9.
        sb.append(trailer(txs.size() + 2, sum, bicChecksum, ibanChecksum));

        return sb.toString();
    }

    private String header() {
        String sb = "1" +
                "0" +
                Fixed200.fill("", 6) +
                LocalDate.now().getYear() +
                Fixed200.fill(DATEI_KENNZEICHEN, 8) +
                BEWIRTSCHAFTER +
                Fixed200.fill("", 7) +
                formatDate6(LocalDate.now()) +
                Fixed200.fill("", 6) +
                " " + // Blank = Satzlaenge 570
                "E" +
                "N" +
                Fixed200.fill("", 60) +
                Fixed200.fill("HKRService", 20) +
                "00000000";
        return padRecord(sb) + "\n";
    }

    private String satz2(F15zTransaction tx, int laufendeNummer) {

        // SK2 Kernfelder gem. Anlage 2, fuer den in HKRService genutzten Teilumfang.
        String sb = "2" +
                "0" +
                Fixed200.fill("", 2) +
                Fixed200.fill("HKRService", 25) +
                buildBelegnummer(laufendeNummer) +
                vslCode(tx) +
                "0" +
                BEWIRTSCHAFTER +
                "0000" +
                "0000000001" +
                "0000000000" +
                "101" +
                Fixed200.fill(tx.getBelegnummer(), 12) +
                "00000" +
                "H22" +
                Fixed200.fill(defaultIfBlank(tx.getTitel(), "EMPF"), 27) +
                Fixed200.fill("", 27) +
                Fixed200.fill("", 27) +
                Fixed200.fill("", 3) +
                Fixed200.fill("", 27) +
                "H01" +
                "00000000" +
                "0000000000" +
                Fixed200.fill("", 27) +
                "100" +
                Fixed200.num(nullSafeAmount(tx), 13) +
                formatDate6(tx.getFaelligkeit()) +
                "0" +
                "00000000" +
                "H32" +
                Fixed200.fill(defaultIfBlank(tx.getTitel(), "F15Z"), 27) +
                "H02" +
                Fixed200.fill("", 25) +
                "H12" +
                Fixed200.fill("", 25) +
                "104" +
                " " +
                "000000000000000" +
                "0000000000" +
                "H82" +
                Fixed200.fill("", 15) +
                "E55" +
                Fixed200.fill("", 27) +
                Fixed200.fill("", 27) +
                Fixed200.fill("", 27) +
                Fixed200.fill("", 27) +
                Fixed200.fill("", 27) +
                " " +
                " " +
                Fixed200.fill("", 8) +
                "BIC" +
                Fixed200.fill(tx.getBic(), 11) +
                "IBAN" +
                Fixed200.fill(tx.getIban(), 34);

        return padRecord(sb);
    }

    private String trailer(int countIncludingEnvelope, BigDecimal sum, long bicChecksum, long ibanChecksum) {
        String sb = "9" +
                "0" +
                Fixed200.fill("", 6) +
                LocalDate.now().getYear() +
                Fixed200.fill(DATEI_KENNZEICHEN, 8) +
                BEWIRTSCHAFTER +
                Fixed200.num(sum, 14) +
                String.format("%05d", countIncludingEnvelope) +
                "000000000000000" +
                "000000000000000" +
                Fixed200.fill("", 16) +
                String.format("%020d", bicChecksum) +
                String.format("%020d", ibanChecksum);
        return padRecord(sb);
    }

    private String vslCode(F15zTransaction tx) {
        if (tx.getType() == null) {
            return "52000";
        }
        return switch (tx.getType()) {
            case AUSZ -> "52000";
            case EINZ -> "53100";
            case UMB -> "68500";
            case RES -> "41000";
        };
    }

    private BigDecimal nullSafeAmount(F15zTransaction tx) {
        return tx.getBetrag() == null ? BigDecimal.ZERO : tx.getBetrag();
    }

    private String buildBelegnummer(int laufendeNummer) {
        LocalDate now = LocalDate.now();
        int yearLastDigit = now.getYear() % 10;
        return String.format("%02d%02d%d%03d", now.getDayOfMonth(), now.getMonthValue(), yearLastDigit, laufendeNummer);
    }

    private String formatDate6(LocalDate date) {
        if (date == null) {
            return "000000";
        }
        return String.format("%02d%02d%02d", date.getDayOfMonth(), date.getMonthValue(), date.getYear() % 100);
    }

    private String defaultIfBlank(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private long calculateBicChecksum(String bic) {
        if (bic == null || bic.isEmpty()) {
            return 0;
        }
        long checksum = 0;
        for (char c : bic.toCharArray()) {
            checksum += c;
        }
        return checksum;
    }

    private long calculateIbanChecksum(String iban) {
        if (iban == null || iban.isEmpty()) {
            return 0;
        }
        long checksum = 0;
        for (char c : iban.toCharArray()) {
            checksum += c;
        }
        return checksum;
    }

    private String padRecord(String line) {
        if (line.length() >= RECORD_LENGTH) {
            return line.substring(0, RECORD_LENGTH);
        }
        return line + " ".repeat(RECORD_LENGTH - line.length());
    }
}