package com.awin.merchantdashboard.period;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class PercentageChange {

    private static final int SCALE = 2;
    private static final BigDecimal ONE_HUNDRED = BigDecimal.valueOf(100);

    private PercentageChange() {}

    /**
     * {@code (current - previous) / previous * 100}, scale 2, half up. Null when previous is 0.
     */
    public static BigDecimal of(BigDecimal current, BigDecimal previous) {
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return current.subtract(previous).multiply(ONE_HUNDRED).divide(previous, SCALE, RoundingMode.HALF_UP);
    }

    public static BigDecimal of(long current, long previous) {
        return of(BigDecimal.valueOf(current), BigDecimal.valueOf(previous));
    }
}
