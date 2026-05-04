package de.graube.hkrservice.validation;

import de.graube.hkrservice.parser.F15zFileParser;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class F15zValidatorTest {

    private final F15zValidator validator = new F15zValidator();

    @Test
    void acceptsMatchingCountAndSum() {
        F15zFileParser.Parsed parsed = parsed(List.of("10.00", "20.00"), 2, "30.00");

        assertDoesNotThrow(() -> validator.validate(parsed));
    }

    @Test
    void rejectsCountMismatch() {
        F15zFileParser.Parsed parsed = parsed(List.of("10.00", "20.00"), 3, "30.00");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.validate(parsed));
        assertEquals("Count mismatch", ex.getMessage());
    }

    @Test
    void rejectsSumMismatch() {
        F15zFileParser.Parsed parsed = parsed(List.of("10.00", "20.00"), 2, "31.00");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> validator.validate(parsed));
        assertEquals("Sum mismatch", ex.getMessage());
    }

    private F15zFileParser.Parsed parsed(List<String> values, int count, String sum) {
        F15zFileParser.Parsed parsed = new F15zFileParser.Parsed();
        parsed.amounts.addAll(values.stream().map(BigDecimal::new).toList());
        parsed.trailerCount = count;
        parsed.trailerSum = new BigDecimal(sum);
        return parsed;
    }
}

