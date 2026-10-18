package com.awin.merchantdashboard.period;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

@Component
public class PeriodResolver {

    private final Clock clock;

    public PeriodResolver(Clock clock) {
        this.clock = clock;
    }

    public PeriodWindows resolve(Period period) {
        LocalDate today = LocalDate.ofInstant(clock.instant(), ZoneOffset.UTC);
        LocalDate yesterday = today.minusDays(1);
        return switch (period) {
            case LAST_7_DAYS -> rolling(yesterday, 7);
            case LAST_30_DAYS -> rolling(yesterday, 30);
            case LAST_90_DAYS -> rolling(yesterday, 90);
            case LAST_MONTH -> lastMonth(today);
            case THIS_MONTH -> thisMonth(today, yesterday);
        };
    }

    private static PeriodWindows rolling(LocalDate yesterday, int days) {
        LocalDate currentFrom = yesterday.minusDays(days - 1L);
        DateRange current = new DateRange(currentFrom, yesterday);
        LocalDate previousTo = currentFrom.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1L);
        return new PeriodWindows(current, new DateRange(previousFrom, previousTo));
    }

    private static PeriodWindows lastMonth(LocalDate today) {
        YearMonth currentMonth = YearMonth.from(today).minusMonths(1);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        return new PeriodWindows(monthRange(currentMonth), monthRange(previousMonth));
    }

    private static PeriodWindows thisMonth(LocalDate today, LocalDate yesterday) {
        LocalDate firstOfMonth = today.withDayOfMonth(1);
        if (yesterday.isBefore(firstOfMonth)) {
            DateRange empty = new DateRange(firstOfMonth, yesterday);
            return new PeriodWindows(empty, empty);
        }
        DateRange current = new DateRange(firstOfMonth, yesterday);
        long days = ChronoUnit.DAYS.between(current.from(), current.to()) + 1;
        LocalDate previousTo = firstOfMonth.minusDays(1);
        LocalDate previousFrom = previousTo.minusDays(days - 1);
        return new PeriodWindows(current, new DateRange(previousFrom, previousTo));
    }

    private static DateRange monthRange(YearMonth month) {
        return new DateRange(month.atDay(1), month.atEndOfMonth());
    }
}
