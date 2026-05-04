package de.graube.hkrservice.validation;

import de.graube.hkrservice.parser.F15zFileParser;

import java.math.BigDecimal;

/**
 * Validiert Konsistenz zwischen Datensaetzen und Trailer einer F15Z-Datei.
 */
public class F15zValidator {

    /**
     * Prueft Anzahl und Summe der geparsten Datensaetze gegen den Trailer.
     *
     * @param p geparste Datei
     */
    public void validate(F15zFileParser.Parsed p) {

        BigDecimal calc = p.amounts.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int expectedCount = p.trailerIncludesEnvelope
                ? p.amounts.size() + 2
                : p.amounts.size();

        if (expectedCount != p.trailerCount)
            throw new RuntimeException("Count mismatch");

        if (calc.compareTo(p.trailerSum) != 0)
            throw new RuntimeException("Sum mismatch");
    }
}
