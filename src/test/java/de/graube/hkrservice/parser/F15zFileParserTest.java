package de.graube.hkrservice.parser;

import de.graube.hkrservice.generator.F15zGenerator;
import de.graube.hkrservice.model.F15zTransaction;
import de.graube.hkrservice.model.VslType;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class F15zFileParserTest {

    private final F15zFileParser parser = new F15zFileParser();

    @Test
    void parsesGeneratedFixedWidthFile() {
        F15zGenerator generator = new F15zGenerator();
        String file = generator.build(List.of(tx("BLG1001", "12.34"), tx("BLG1002", "50.00")));

        F15zFileParser.Parsed parsed = parser.parse(file);

        assertEquals(2, parsed.records.size());
        assertEquals("BLG1001", parsed.records.get(0).belegnummer);
        assertEquals(new BigDecimal("12.34"), parsed.records.get(0).amount);
        assertEquals(4, parsed.trailerCount);
        assertEquals(new BigDecimal("62.34"), parsed.trailerSum);
        assertTrue(parsed.trailerIncludesEnvelope);
    }

    @Test
    void parsesReturnLines() {
        String content = "2|BLG2001|OK|accepted\n" +
                "2|BLG2002|ERROR|failed";

        F15zFileParser.Parsed parsed = parser.parse(content);

        assertEquals(2, parsed.records.size());
        assertEquals("BLG2001", parsed.records.get(0).belegnummer);
        assertEquals("OK", parsed.records.get(0).status);
        assertEquals("accepted", parsed.records.get(0).message);
        assertEquals("ERROR", parsed.records.get(1).status);
    }

    @Test
    void throwsForInvalidFixedWidthTrailer() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> parser.parse("9")
        );

        assertEquals("Invalid F15z trailer line: 9", ex.getMessage());
    }

    private F15zTransaction tx(String belegnummer, String amount) {
        F15zTransaction tx = new F15zTransaction();
        tx.setType(VslType.AUSZ);
        tx.setBelegnummer(belegnummer);
        tx.setFaelligkeit(LocalDate.of(2026, 5, 1));
        tx.setBetrag(new BigDecimal(amount));
        tx.setIban("DE89370400440532013000");
        tx.setBic("COBADEFFXXX");
        tx.setTitel("TITEL");
        tx.setObjektkonto("OBJ1");
        return tx;
    }
}

