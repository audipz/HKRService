package de.graube.hkrservice.generator;

import de.graube.hkrservice.model.F15zTransaction;
import de.graube.hkrservice.util.Fixed200;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Erzeugt F15Z-Dateiinhalte im Festlaengenformat.
 */
public class F15zGenerator {

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

        for (F15zTransaction tx : txs) {
            sb.append(satz2(tx)).append("\n");
            sum = sum.add(tx.betrag);
        }

        sb.append(trailer(txs.size(), sum));

        return sb.toString();
    }

    private String header() {
        return "1" +
                Fixed200.fill(LocalDateTime.now().toLocalDate().toString().replace("-", ""), 8) +
                Fixed200.fill("120000", 6) +
                Fixed200.fill("12345678", 8) +
                Fixed200.fill("RUN01", 20) +
                " ".repeat(200 - 43) + "\n";
    }

    private String satz2(F15zTransaction tx) {
        StringBuilder sb = new StringBuilder();

        sb.append("2");
        sb.append(Fixed200.fill(tx.type.name(), 4));
        sb.append(Fixed200.fill(tx.belegnummer, 10));
        sb.append(Fixed200.fill(tx.faelligkeit.toString().replace("-", ""), 8));
        sb.append(Fixed200.num(tx.betrag, 13));
        sb.append(Fixed200.fill("EUR", 3));
        sb.append(Fixed200.fill(tx.iban, 34));
        sb.append(Fixed200.fill(tx.bic, 11));
        sb.append(Fixed200.fill(tx.titel, 10));
        sb.append(Fixed200.fill(tx.objektkonto, 10));
        sb.append(Fixed200.fill("", 20));

        while (sb.length() < 200) sb.append(" ");

        return sb.toString();
    }

    private String trailer(int count, BigDecimal sum) {
        return "9" +
                String.format("%06d", count) +
                Fixed200.num(sum, 15) +
                " ".repeat(200 - 22);
    }
}