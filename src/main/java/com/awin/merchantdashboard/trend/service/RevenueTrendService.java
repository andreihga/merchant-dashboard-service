package com.awin.merchantdashboard.trend.service;

import com.awin.merchantdashboard.api.model.Period;
import com.awin.merchantdashboard.api.model.RevenueTrend;
import com.awin.merchantdashboard.api.model.RevenueTrendPoint;
import com.awin.merchantdashboard.kpi.model.SaleStatus;
import com.awin.merchantdashboard.kpi.model.SaleTransaction;
import com.awin.merchantdashboard.kpi.repository.SaleTransactionRepository;
import com.awin.merchantdashboard.merchant.repository.MerchantRepository;
import com.awin.merchantdashboard.period.DateRange;
import com.awin.merchantdashboard.period.PercentageChange;
import com.awin.merchantdashboard.period.PeriodResolver;
import com.awin.merchantdashboard.period.PeriodWindows;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevenueTrendService {

    private static final List<SaleStatus> COUNTED = List.of(SaleStatus.PENDING, SaleStatus.APPROVED);
    private static final int SCALE = 2;

    private final SaleTransactionRepository sales;
    private final MerchantRepository merchants;
    private final PeriodResolver periodResolver;

    public RevenueTrendService(
            SaleTransactionRepository sales, MerchantRepository merchants, PeriodResolver periodResolver) {
        this.sales = sales;
        this.merchants = merchants;
        this.periodResolver = periodResolver;
    }

    /** The caller must already have passed the merchant access check. */
    @Transactional(readOnly = true)
    public RevenueTrend getRevenueTrend(long merchantId, Period period) {
        PeriodWindows windows = periodResolver.resolve(com.awin.merchantdashboard.period.Period.valueOf(period.name()));
        Map<LocalDate, BigDecimal> byDay = revenueByDay(merchantId, windows);
        List<RevenueTrendPoint> current = dailyPoints(windows.current(), byDay);
        List<RevenueTrendPoint> previous = dailyPoints(windows.previous(), byDay);
        BigDecimal total = sum(current);
        String currency = merchants.findById(merchantId).orElseThrow().getCurrency();
        RevenueTrend trend = new RevenueTrend(period, total, currency, current, previous);
        trend.setChangePercent(PercentageChange.of(total, sum(previous)));
        return trend;
    }

    private Map<LocalDate, BigDecimal> revenueByDay(long merchantId, PeriodWindows windows) {
        List<SaleTransaction> rows = sales.findCountedInRange(
                merchantId,
                COUNTED,
                windows.previous().startInstant(),
                windows.current().endExclusiveInstant());
        Map<LocalDate, BigDecimal> byDay = new HashMap<>();
        for (SaleTransaction row : rows) {
            LocalDate day = LocalDate.ofInstant(row.getTransactionDate(), ZoneOffset.UTC);
            byDay.merge(day, row.getSaleAmount(), BigDecimal::add);
        }
        return byDay;
    }

    private static List<RevenueTrendPoint> dailyPoints(DateRange range, Map<LocalDate, BigDecimal> byDay) {
        List<RevenueTrendPoint> points = new ArrayList<>();
        for (LocalDate day = range.from(); !day.isAfter(range.to()); day = day.plusDays(1)) {
            BigDecimal amount = byDay.getOrDefault(day, BigDecimal.ZERO).setScale(SCALE, RoundingMode.HALF_UP);
            points.add(new RevenueTrendPoint(day, amount));
        }
        return points;
    }

    private static BigDecimal sum(List<RevenueTrendPoint> points) {
        BigDecimal total = BigDecimal.ZERO.setScale(SCALE, RoundingMode.HALF_UP);
        for (RevenueTrendPoint point : points) {
            total = total.add(point.getValue());
        }
        return total;
    }
}
