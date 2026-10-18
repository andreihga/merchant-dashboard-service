package com.awin.merchantdashboard.period;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class PeriodResolverTest {

    @Test
    void given_today_when_resolve_last_7_days_then_rolling_window_ending_yesterday() {
        PeriodResolver resolver = resolverAt("2026-10-18T12:00:00Z");

        PeriodWindows windows = resolver.resolve(Period.LAST_7_DAYS);

        assertThat(windows.current()).isEqualTo(range("2026-10-11", "2026-10-17"));
        assertThat(windows.previous()).isEqualTo(range("2026-10-04", "2026-10-10"));
    }

    @Test
    void given_today_when_resolve_last_30_days_then_rolling_window_ending_yesterday() {
        PeriodResolver resolver = resolverAt("2026-10-18T12:00:00Z");

        PeriodWindows windows = resolver.resolve(Period.LAST_30_DAYS);

        assertThat(windows.current()).isEqualTo(range("2026-09-18", "2026-10-17"));
        assertThat(windows.previous()).isEqualTo(range("2026-08-19", "2026-09-17"));
    }

    @Test
    void given_today_when_resolve_last_90_days_then_rolling_window_ending_yesterday() {
        PeriodResolver resolver = resolverAt("2026-10-18T12:00:00Z");

        PeriodWindows windows = resolver.resolve(Period.LAST_90_DAYS);

        assertThat(windows.current()).isEqualTo(range("2026-07-20", "2026-10-17"));
        assertThat(windows.previous()).isEqualTo(range("2026-04-21", "2026-07-19"));
    }

    @Test
    void given_today_when_resolve_last_month_then_previous_calendar_month_compared_with_month_before() {
        PeriodResolver resolver = resolverAt("2026-10-18T12:00:00Z");

        PeriodWindows windows = resolver.resolve(Period.LAST_MONTH);

        assertThat(windows.current()).isEqualTo(range("2026-09-01", "2026-09-30"));
        assertThat(windows.previous()).isEqualTo(range("2026-08-01", "2026-08-31"));
    }

    @Test
    void given_leap_year_march_when_resolve_last_month_then_february_has_29_days() {
        PeriodResolver resolver = resolverAt("2024-03-15T00:00:00Z");

        PeriodWindows windows = resolver.resolve(Period.LAST_MONTH);

        assertThat(windows.current()).isEqualTo(range("2024-02-01", "2024-02-29"));
        assertThat(windows.previous()).isEqualTo(range("2024-01-01", "2024-01-31"));
    }

    @Test
    void given_mid_month_when_resolve_this_month_then_from_first_through_yesterday_with_equal_length_previous() {
        PeriodResolver resolver = resolverAt("2026-10-18T12:00:00Z");

        PeriodWindows windows = resolver.resolve(Period.THIS_MONTH);

        assertThat(windows.current()).isEqualTo(range("2026-10-01", "2026-10-17"));
        assertThat(windows.previous()).isEqualTo(range("2026-09-14", "2026-09-30"));
        assertThat(windows.current().isEmpty()).isFalse();
    }

    @Test
    void given_first_of_month_when_resolve_this_month_then_empty_current_and_previous() {
        PeriodResolver resolver = resolverAt("2026-10-01T08:00:00Z");

        PeriodWindows windows = resolver.resolve(Period.THIS_MONTH);

        assertThat(windows.current()).isEqualTo(range("2026-10-01", "2026-09-30"));
        assertThat(windows.previous()).isEqualTo(range("2026-10-01", "2026-09-30"));
        assertThat(windows.current().isEmpty()).isTrue();
        assertThat(windows.previous().isEmpty()).isTrue();
    }

    private static PeriodResolver resolverAt(String instant) {
        Clock clock = Clock.fixed(Instant.parse(instant), ZoneOffset.UTC);
        return new PeriodResolver(clock);
    }

    private static DateRange range(String from, String to) {
        return new DateRange(LocalDate.parse(from), LocalDate.parse(to));
    }
}
