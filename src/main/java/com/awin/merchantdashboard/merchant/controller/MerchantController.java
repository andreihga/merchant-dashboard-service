package com.awin.merchantdashboard.merchant.controller;

import com.awin.merchantdashboard.api.MerchantsApi;
import com.awin.merchantdashboard.api.model.Merchant;
import com.awin.merchantdashboard.merchant.service.MerchantService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RestController;

@RestController
class MerchantController implements MerchantsApi {

    private final MerchantService merchantService;

    MerchantController(MerchantService merchantService) {
        this.merchantService = merchantService;
    }

    @Override
    public ResponseEntity<List<Merchant>> listMerchants() {
        long userId = Long.parseLong(
                SecurityContextHolder.getContext().getAuthentication().getName());
        return ResponseEntity.ok(merchantService.listMerchants(userId));
    }
}
