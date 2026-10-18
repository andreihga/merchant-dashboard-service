package com.awin.merchantdashboard.kpi.service;

import com.awin.merchantdashboard.api.model.CountKpi;
import com.awin.merchantdashboard.api.model.KpiMetric;
import com.awin.merchantdashboard.api.model.Period;
import com.awin.merchantdashboard.kpi.model.CountTotals;
import com.awin.merchantdashboard.kpi.model.SaleStatus;
import com.awin.merchantdashboard.kpi.repository.SaleTransactionRepository;
import com.awin.merchantdashboard.period.PercentageChange;
import com.awin.merchantdashboard.period.PeriodResolver;
import com.awin.merchantdashboard.period.PeriodWindows;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransactionsKpiService {

    private static final List<SaleStatus> COUNTED = List.of(SaleStatus.PENDING, SaleStatus.APPROVED);

    private final SaleTransactionRepository sales;
    private final PeriodResolver periodResolver;

    public TransactionsKpiService(SaleTransactionRepository sales, PeriodResolver periodResolver) {
        this.sales = sales;
        this.periodResolver = periodResolver;
    }

    /** The caller must already have passed the merchant access check. */
    @Transactional(readOnly = true)
    public CountKpi transactions(long merchantId, Period period) {
        PeriodWindows windows = periodResolver.resolve(com.awin.merchantdashboard.period.Period.valueOf(period.name()));
        CountTotals totals = sales.countSales(
                merchantId,
                COUNTED,
                windows.previous().startInstant(),
                windows.current().startInstant(),
                windows.current().endExclusiveInstant());
        CountKpi kpi = new CountKpi(
                KpiMetric.TRANSACTIONS,
                period,
                windows.current().from(),
                windows.current().to(),
                totals.current(),
                totals.previous());
        kpi.setChangePercent(PercentageChange.of(totals.current(), totals.previous()));
        return kpi;
    }
}
