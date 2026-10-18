package com.awin.merchantdashboard.period;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PercentageChangeTest {

    @Test
    void returnsNullWhenPreviousIsZero() {
        assertThat(PercentageChange.of(new BigDecimal("100.00"), BigDecimal.ZERO))
                .isNull();
        assertThat(PercentageChange.of(42L, 0L)).isNull();
    }

    @Test
    void computesScaleTwoHalfUp() {
        assertThat(PercentageChange.of(new BigDecimal("150.00"), new BigDecimal("100.00")))
                .isEqualByComparingTo("50.00");
        assertThat(PercentageChange.of(new BigDecimal("100.00"), new BigDecimal("300.00")))
                .isEqualByComparingTo("-66.67");
        assertThat(PercentageChange.of(200L, 100L)).isEqualByComparingTo("100.00");
    }
}
