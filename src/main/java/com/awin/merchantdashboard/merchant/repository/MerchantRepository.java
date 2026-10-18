package com.awin.merchantdashboard.merchant.repository;

import com.awin.merchantdashboard.merchant.model.Merchant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {}
