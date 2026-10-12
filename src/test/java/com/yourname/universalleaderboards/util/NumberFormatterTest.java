package com.yourname.universalleaderboards.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class NumberFormatterTest {

    @Test
    void parsesPlainInteger() {
        assertEquals(12345.0, NumberFormatter.parse("12345"));
    }

    @Test
    void parsesCommaSeparated() {
        assertEquals(1250000.0, NumberFormatter.parse("1,250,000"));
    }

    @Test
    void parsesCurrencyPrefixed() {
        assertEquals(1250000.0, NumberFormatter.parse("$1,250,000"));
    }

    @Test
    void parsesShorthandSuffix() {
        assertEquals(1250000.0, NumberFormatter.parse("1.25M"));
        assertEquals(1000.0, NumberFormatter.parse("1K"));
        assertEquals(1250000000.0, NumberFormatter.parse("1.25B"));
    }

    @Test
    void parsesNegativeNumbers() {
        assertEquals(-42.0, NumberFormatter.parse("-42"));
    }

    @Test
    void parsesDecimal() {
        assertEquals(3.14, NumberFormatter.parse("3.14"));
    }

    @Test
    void returnsNullForInvalidValues() {
        assertNull(NumberFormatter.parse("N/A"));
        assertNull(NumberFormatter.parse(""));
        assertNull(NumberFormatter.parse(null));
        assertNull(NumberFormatter.parse("NaN"));
        assertNull(NumberFormatter.parse("-"));
    }

    @Test
    void formatsShortMode() {
        assertEquals("1.25M", NumberFormatter.format(1_250_000, NumberFormatter.Mode.SHORT, 2));
        assertEquals("1K", NumberFormatter.format(1_000, NumberFormatter.Mode.SHORT, 2));
        assertEquals("999", NumberFormatter.format(999, NumberFormatter.Mode.SHORT, 2));
    }

    @Test
    void formatsCommaMode() {
        assertEquals("1,250,000", NumberFormatter.format(1_250_000, NumberFormatter.Mode.COMMA, 2));
    }
}
