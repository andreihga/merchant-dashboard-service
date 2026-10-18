package com.awin.merchantdashboard.merchant.service;

import com.awin.merchantdashboard.api.model.Merchant;
import com.awin.merchantdashboard.merchant.repository.MerchantRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MerchantService {

    private final MerchantRepository merchants;

    public MerchantService(MerchantRepository merchants) {
        this.merchants = merchants;
    }

    @Transactional(readOnly = true)
    public List<Merchant> listAccessible(long userId) {
        // The entity shares the generated model's simple name, so it is never named here.
        return merchants.findAccessibleByUser(userId).stream()
                .map(m -> new Merchant(m.getId(), m.getName(), m.getCurrency()))
                .toList();
    }
}
