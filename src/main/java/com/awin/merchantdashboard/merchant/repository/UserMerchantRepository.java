package com.awin.merchantdashboard.merchant.repository;

import com.awin.merchantdashboard.merchant.model.UserMerchant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMerchantRepository extends JpaRepository<UserMerchant, Long> {

    boolean existsByUserIdAndMerchantId(Long userId, Long merchantId);
}
