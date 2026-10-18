package com.awin.merchantdashboard.trend.controller;

import com.awin.merchantdashboard.api.RevenueTrendApi;
import com.awin.merchantdashboard.api.model.Period;
import com.awin.merchantdashboard.api.model.RevenueTrend;
import com.awin.merchantdashboard.merchant.service.MerchantAccessGuard;
import com.awin.merchantdashboard.trend.service.RevenueTrendService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
class RevenueTrendController implements RevenueTrendApi {

    private final MerchantAccessGuard guard;
    private final RevenueTrendService service;

    RevenueTrendController(MerchantAccessGuard guard, RevenueTrendService service) {
        this.guard = guard;
        this.service = service;
    }

    @Override
    public ResponseEntity<RevenueTrend> getRevenueTrend(Long merchantId, Period period) {
        long userId = Long.parseLong(
                SecurityContextHolder.getContext().getAuthentication().getName());
        guard.check(userId, merchantId);
        return ResponseEntity.ok(service.revenueTrend(merchantId, period));
    }
}
