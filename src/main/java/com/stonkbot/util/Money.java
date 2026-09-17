package com.stonkbot.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

public final class Money {
    private Money() {
    }

    public static String usd(BigDecimal value) {
        BigDecimal v = value.setScale(2, RoundingMode.HALF_UP);
        String body = String.format(Locale.US, "%,.2f", v.abs());
        return (v.signum() < 0 ? "-$" : "$") + body;
    }

    public static String signedUsd(BigDecimal value) {
        BigDecimal v = value.setScale(2, RoundingMode.HALF_UP);
        if (v.signum() > 0) {
            return "+" + usd(v);
        }
        if (v.signum() < 0) {
            return "-" + usd(v.abs());
        }
        return usd(v);
    }

    public static String pct(BigDecimal value) {
        BigDecimal v = value.setScale(2, RoundingMode.HALF_UP);
        String body = String.format(Locale.US, "%.2f", v.abs());
        if (v.signum() > 0) {
            return "+" + body + "%";
        }
        if (v.signum() < 0) {
            return "-" + body + "%";
        }
        return body + "%";
    }

    public static String shares(BigDecimal value) {
        BigDecimal v = value.stripTrailingZeros();
        if (v.scale() < 0) {
            v = v.setScale(0);
        }
        return v.toPlainString();
    }
}
