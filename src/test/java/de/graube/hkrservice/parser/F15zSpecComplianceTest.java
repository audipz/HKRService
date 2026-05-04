package de.graube.hkrservice.parser;

import de.graube.hkrservice.generator.F15zGenerator;
import de.graube.hkrservice.model.F15zTransaction;
import de.graube.hkrservice.model.VslType;
import de.graube.hkrservice.validation.F15zValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class F15zSpecComplianceTest {

    private F15zGenerator generator;
    private F15zFileParser parser;

    @BeforeEach
    void setUp() {
        generator = new F15zGenerator();
        parser = new F15zFileParser();
    }

    @Test
    void generatesFixed570ByteRecords() {
        String file = generator.build(List.of(tx("BLG1001", VslType.AUSZ, "12.34")));
        String[] lines = file.split("\\r?\\n");

        assertEquals(3, lines.length);
        assertEquals(570, lines[0].length(), "SK1 muss 570 Byte haben");
        assertEquals(570, lines[1].length(), "SK2 muss 570 Byte haben");
        assertEquals(570, lines[2].length(), "SK9 muss 570 Byte haben");
    }

    @Test
    void usesCorrectRecordKinds() {
        String file = generator.build(List.of(tx("BLG1001", VslType.AUSZ, "12.34")));
        String[] lines = file.split("\\r?\\n");

        assertTrue(lines[0].startsWith("1"));
        assertTrue(lines[1].startsWith("2"));
        assertTrue(lines[2].startsWith("9"));
    }

    @Test
    void writesVslCodesAsFiveDigitNumbers() {
        String fileAusz = generator.build(List.of(tx("BLG1001", VslType.AUSZ, "10.00")));
        String fileEinz = generator.build(List.of(tx("BLG1002", VslType.EINZ, "10.00")));
        String fileUmb = generator.build(List.of(tx("BLG1003", VslType.UMB, "10.00")));
        String fileRes = generator.build(List.of(tx("BLG1004", VslType.RES, "10.00")));

        assertEquals("52000", fileAusz.split("\\r?\\n")[1].substring(37, 42));
        assertEquals("53100", fileEinz.split("\\r?\\n")[1].substring(37, 42));
        assertEquals("68500", fileUmb.split("\\r?\\n")[1].substring(37, 42));
        assertEquals("41000", fileRes.split("\\r?\\n")[1].substring(37, 42));
    }

    @Test
    void keepsBusinessBelegnummerInKassenzeichenField() {
        String file = generator.build(List.of(tx("BLG7777", VslType.AUSZ, "50.00")));
        F15zFileParser.Parsed parsed = parser.parse(file);

        assertEquals("BLG7777", parsed.records.get(0).belegnummer);
    }

    @Test
    void trailerContainsEnvelopeCount() {
        String file = generator.build(List.of(
                tx("BLG1001", VslType.AUSZ, "10.00"),
                tx("BLG1002", VslType.AUSZ, "20.00")
        ));

        F15zFileParser.Parsed parsed = parser.parse(file);
        assertEquals(4, parsed.trailerCount, "2 Nutzdatensaetze + SK1 + SK9");
        assertTrue(parsed.trailerIncludesEnvelope);
    }

    @Test
    void parserAndValidatorAcceptGeneratedSpecLikeFile() {
        String file = generator.build(List.of(
                tx("BLG1001", VslType.AUSZ, "12.34"),
                tx("BLG1002", VslType.AUSZ, "50.00")
        ));

        F15zFileParser.Parsed parsed = parser.parse(file);

        assertEquals(2, parsed.records.size());
        assertEquals(new BigDecimal("62.34"), parsed.trailerSum);

        new F15zValidator().validate(parsed);
    }

    @Test
    void amountIsEncodedAsCentValueWithoutDecimalSeparator() {
        String file = generator.build(List.of(tx("BLG1001", VslType.AUSZ, "123.45")));
        String line = file.split("\\r?\\n")[1];

        String amount = line.substring(260, 273);
        assertEquals("0000000012345", amount);
        assertFalse(amount.contains("."));
        assertFalse(amount.contains(","));
    }

    @Test
    void calculatesBicAndIbanChecksums() {
        String file = generator.build(List.of(tx("BLG1001", VslType.AUSZ, "10.00")));
        F15zFileParser.Parsed parsed = parser.parse(file);

        // BIC "COBADEFFXXX" Checksum wird berechnet als Summe der Zeichencodes
        assertEquals(818, parsed.bicChecksum, "BIC-Checksum sollte 818 sein fuer COBADEFFXXX");

        // IBAN "DE89370400440532013000" Checksum
        long expectedIbanSum = 0;
        for (char c : "DE89370400440532013000".toCharArray()) {
            expectedIbanSum += c;
        }
        assertEquals(expectedIbanSum, parsed.ibanChecksum, "IBAN-Checksum mismatch");
    }

    @Test
    void supportsPipeSeparatedReturnFiles() {
        String content = "2|BLG2001|OK|accepted\n2|BLG2002|ERROR|failed\n9|2|0.00";
        F15zFileParser.Parsed parsed = parser.parse(content);

        assertEquals(2, parsed.records.size());
        assertEquals("BLG2001", parsed.records.get(0).belegnummer);
        assertEquals("OK", parsed.records.get(0).status);
        assertEquals(2, parsed.trailerCount);
        assertFalse(parsed.trailerIncludesEnvelope);
    }

    private F15zTransaction tx(String belegnummer, VslType type, String amount) {
        F15zTransaction tx = new F15zTransaction();
        tx.setType(type);
        tx.setBelegnummer(belegnummer);
        tx.setFaelligkeit(LocalDate.of(2026, 5, 4));
        tx.setBetrag(new BigDecimal(amount));
        tx.setIban("DE89370400440532013000");
        tx.setBic("COBADEFFXXX");
        tx.setTitel("TEST");
        tx.setObjektkonto("OBJ-1");
        return tx;
    }
}
