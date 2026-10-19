package com.awin.merchantdashboard.kpi.controller;

import com.awin.merchantdashboard.api.ClicksKpiApi;
import com.awin.merchantdashboard.api.model.CountKpi;
import com.awin.merchantdashboard.api.model.Period;
import com.awin.merchantdashboard.kpi.service.ClicksKpiService;
import com.awin.merchantdashboard.merchant.service.MerchantAccessGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
class ClicksKpiController implements ClicksKpiApi {

    private final MerchantAccessGuard guard;
    private final ClicksKpiService service;

    ClicksKpiController(MerchantAccessGuard guard, ClicksKpiService service) {
        this.guard = guard;
        this.service = service;
    }

    @Override
    public ResponseEntity<CountKpi> getClicksKpi(Long merchantId, Period period) {
        long userId = Long.parseLong(
                SecurityContextHolder.getContext().getAuthentication().getName());
        guard.check(userId, merchantId);
        return ResponseEntity.ok(service.getClicks(merchantId, period));
    }
}
