package com.awin.merchantdashboard.period;

import java.time.LocalDate;

/** Inclusive UTC date window. Empty when {@code from} is after {@code to}. */
public record DateRange(LocalDate from, LocalDate to) {

    public boolean isEmpty() {
        return from.isAfter(to);
    }
}
