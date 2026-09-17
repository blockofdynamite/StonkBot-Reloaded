package com.stonkbot.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MoneyTest {
    @Test
    void formatsWholeDollars() {
        assertEquals("$1,000.00", Money.usd(new BigDecimal("1000")));
    }

    @Test
    void formatsCents() {
        assertEquals("$1,234.56", Money.usd(new BigDecimal("1234.56")));
    }

    @Test
    void roundsHalfUp() {
        assertEquals("$1,000,001.00", Money.usd(new BigDecimal("1000000.999")));
    }

    @Test
    void formatsNegatives() {
        assertEquals("-$12.34", Money.usd(new BigDecimal("-12.34")));
    }

    @Test
    void formatsZero() {
        assertEquals("$0.00", Money.usd(BigDecimal.ZERO));
    }

    @Test
    void signedPositive() {
        assertEquals("+$5.00", Money.signedUsd(new BigDecimal("5")));
    }

    @Test
    void signedNegative() {
        assertEquals("-$5.00", Money.signedUsd(new BigDecimal("-5")));
    }

    @Test
    void signedZero() {
        assertEquals("$0.00", Money.signedUsd(BigDecimal.ZERO));
    }

    @Test
    void pctPositive() {
        assertEquals("+20.00%", Money.pct(new BigDecimal("20")));
    }

    @Test
    void pctNegative() {
        assertEquals("-3.50%", Money.pct(new BigDecimal("-3.5")));
    }

    @Test
    void sharesStripTrailingZeros() {
        assertEquals("10", Money.shares(new BigDecimal("10.0000")));
        assertEquals("0.5", Money.shares(new BigDecimal("0.50")));
        assertEquals("2.1234", Money.shares(new BigDecimal("2.1234")));
    }
}
