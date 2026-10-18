package com.awin.merchantdashboard.merchant.service;

import com.awin.merchantdashboard.merchant.repository.MerchantRepository;
import com.awin.merchantdashboard.merchant.repository.UserMerchantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class MerchantAccessGuard {

    private final MerchantRepository merchants;
    private final UserMerchantRepository links;

    public MerchantAccessGuard(MerchantRepository merchants, UserMerchantRepository links) {
        this.merchants = merchants;
        this.links = links;
    }

    public void check(long userId, long merchantId) {
        if (!merchants.existsById(merchantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Merchant not found");
        }
        if (!links.existsByUserIdAndMerchantId(userId, merchantId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Merchant access denied");
        }
    }
}
