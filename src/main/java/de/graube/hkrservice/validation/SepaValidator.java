package de.graube.hkrservice.validation;

import org.iban4j.IbanFormatException;
import org.iban4j.IbanUtil;

/**
 * Hilfsklasse zur IBAN-Validierung fuer SEPA-Daten.
 */
public class SepaValidator {

    /**
     * Validiert eine IBAN und wirft bei Fehlern eine RuntimeException.
     *
     * @param iban zu validierende IBAN
     */
    public static void validate(String iban) {
        if (iban != null) {
            try {
                IbanUtil.validate(iban);
            } catch (IbanFormatException | org.iban4j.InvalidCheckDigitException | org.iban4j.UnsupportedCountryException e) {
                throw new RuntimeException("Invalid IBAN: " + e.getMessage());
            }
        }
    }
}