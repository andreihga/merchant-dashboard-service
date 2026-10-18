package com.awin.merchantdashboard.todo.repository;

import com.awin.merchantdashboard.todo.model.AffiliateApplication;
import com.awin.merchantdashboard.todo.model.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AffiliateApplicationRepository extends JpaRepository<AffiliateApplication, Long> {

    long countByMerchantIdAndStatus(long merchantId, ApplicationStatus status);
}
