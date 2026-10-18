package com.awin.merchantdashboard.kpi.service;

import com.awin.merchantdashboard.api.model.CountKpi;
import com.awin.merchantdashboard.api.model.KpiMetric;
import com.awin.merchantdashboard.api.model.Period;
import com.awin.merchantdashboard.kpi.model.CountTotals;
import com.awin.merchantdashboard.kpi.repository.ClickDailyRepository;
import com.awin.merchantdashboard.period.PercentageChange;
import com.awin.merchantdashboard.period.PeriodResolver;
import com.awin.merchantdashboard.period.PeriodWindows;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClicksKpiService {

    private final ClickDailyRepository clicks;
    private final PeriodResolver periodResolver;

    public ClicksKpiService(ClickDailyRepository clicks, PeriodResolver periodResolver) {
        this.clicks = clicks;
        this.periodResolver = periodResolver;
    }

    /** The caller must already have passed the merchant access check. */
    @Transactional(readOnly = true)
    public CountKpi clicks(long merchantId, Period period) {
        PeriodWindows windows = periodResolver.resolve(com.awin.merchantdashboard.period.Period.valueOf(period.name()));
        CountTotals totals = clicks.sumClicks(
                merchantId,
                windows.previous().from(),
                windows.current().from(),
                windows.current().to());
        CountKpi kpi = new CountKpi(
                KpiMetric.CLICKS,
                period,
                windows.current().from(),
                windows.current().to(),
                totals.current(),
                totals.previous());
        kpi.setChangePercent(PercentageChange.of(totals.current(), totals.previous()));
        return kpi;
    }
}
