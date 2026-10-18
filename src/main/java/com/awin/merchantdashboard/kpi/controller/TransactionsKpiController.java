package com.awin.merchantdashboard.kpi.controller;

import com.awin.merchantdashboard.api.TransactionsKpiApi;
import com.awin.merchantdashboard.api.model.CountKpi;
import com.awin.merchantdashboard.api.model.Period;
import com.awin.merchantdashboard.kpi.service.TransactionsKpiService;
import com.awin.merchantdashboard.merchant.service.MerchantAccessGuard;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
class TransactionsKpiController implements TransactionsKpiApi {

    private final MerchantAccessGuard guard;
    private final TransactionsKpiService service;

    TransactionsKpiController(MerchantAccessGuard guard, TransactionsKpiService service) {
        this.guard = guard;
        this.service = service;
    }

    @Override
    public ResponseEntity<CountKpi> getTransactionsKpi(Long merchantId, Period period) {
        long userId = Long.parseLong(
                SecurityContextHolder.getContext().getAuthentication().getName());
        guard.check(userId, merchantId);
        return ResponseEntity.ok(service.transactions(merchantId, period));
    }
}
