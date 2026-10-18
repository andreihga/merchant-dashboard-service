package com.awin.merchantdashboard.kpi.service;

import com.awin.merchantdashboard.api.model.KpiMetric;
import com.awin.merchantdashboard.api.model.Period;
import com.awin.merchantdashboard.api.model.RevenueKpi;
import com.awin.merchantdashboard.kpi.model.RevenueTotals;
import com.awin.merchantdashboard.kpi.model.SaleStatus;
import com.awin.merchantdashboard.kpi.repository.SaleTransactionRepository;
import com.awin.merchantdashboard.merchant.repository.MerchantRepository;
import com.awin.merchantdashboard.period.PercentageChange;
import com.awin.merchantdashboard.period.PeriodResolver;
import com.awin.merchantdashboard.period.PeriodWindows;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RevenueKpiService {

    private static final List<SaleStatus> COUNTED = List.of(SaleStatus.PENDING, SaleStatus.APPROVED);
    private static final int SCALE = 2;

    private final SaleTransactionRepository sales;
    private final MerchantRepository merchants;
    private final PeriodResolver periodResolver;

    public RevenueKpiService(
            SaleTransactionRepository sales, MerchantRepository merchants, PeriodResolver periodResolver) {
        this.sales = sales;
        this.merchants = merchants;
        this.periodResolver = periodResolver;
    }

    /** The caller must already have passed the merchant access check. */
    @Transactional(readOnly = true)
    public RevenueKpi revenue(long merchantId, Period period) {
        PeriodWindows windows = periodResolver.resolve(com.awin.merchantdashboard.period.Period.valueOf(period.name()));
        RevenueTotals totals = sales.sumRevenue(
                merchantId,
                COUNTED,
                windows.previous().startInstant(),
                windows.current().startInstant(),
                windows.current().endExclusiveInstant());
        BigDecimal value = totals.current().setScale(SCALE, RoundingMode.HALF_UP);
        BigDecimal previousValue = totals.previous().setScale(SCALE, RoundingMode.HALF_UP);
        String currency = merchants.findById(merchantId).orElseThrow().getCurrency();
        RevenueKpi kpi = new RevenueKpi(
                KpiMetric.REVENUE,
                period,
                windows.current().from(),
                windows.current().to(),
                value,
                previousValue,
                currency);
        kpi.setChangePercent(PercentageChange.of(value, previousValue));
        return kpi;
    }
}
