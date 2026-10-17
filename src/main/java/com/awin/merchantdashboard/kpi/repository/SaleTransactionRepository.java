package com.awin.merchantdashboard.kpi.repository;

import com.awin.merchantdashboard.kpi.model.SaleTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, Long> {}
