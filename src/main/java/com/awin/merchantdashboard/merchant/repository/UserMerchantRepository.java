package com.awin.merchantdashboard.merchant.repository;

import com.awin.merchantdashboard.merchant.model.UserMerchant;
import com.awin.merchantdashboard.merchant.model.UserMerchantId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserMerchantRepository extends JpaRepository<UserMerchant, UserMerchantId> {}
