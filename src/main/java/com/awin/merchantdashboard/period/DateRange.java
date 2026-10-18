package com.awin.merchantdashboard.period;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

/** Inclusive UTC date window. Empty when {@code from} is after {@code to}. */
public record DateRange(LocalDate from, LocalDate to) {

    public boolean isEmpty() {
        return from.isAfter(to);
    }

    /** The first instant of {@code from}, in UTC. */
    public Instant startInstant() {
        return from.atStartOfDay(ZoneOffset.UTC).toInstant();
    }

    /** The first instant after {@code to}, in UTC, so a timestamp is inside when it is before this. */
    public Instant endExclusiveInstant() {
        return to.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    }
}
