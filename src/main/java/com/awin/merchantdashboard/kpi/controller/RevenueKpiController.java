package com.awin.merchantdashboard.kpi.controller;

import com.awin.merchantdashboard.api.RevenueKpiApi;
import com.awin.merchantdashboard.api.model.Period;
import com.awin.merchantdashboard.api.model.RevenueKpi;
import com.awin.merchantdashboard.kpi.service.RevenueKpiService;
import com.awin.merchantdashboard.merchant.service.MerchantAccessGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RevenueKpiController implements RevenueKpiApi {

    private final MerchantAccessGuard guard;
    private final RevenueKpiService service;

    RevenueKpiController(MerchantAccessGuard guard, RevenueKpiService service) {
        this.guard = guard;
        this.service = service;
    }

    @Override
    public ResponseEntity<RevenueKpi> getRevenueKpi(Long merchantId, Period period) {
        long userId = Long.parseLong(
                SecurityContextHolder.getContext().getAuthentication().getName());
        guard.check(userId, merchantId);
        return ResponseEntity.ok(service.revenue(merchantId, period));
    }
}
