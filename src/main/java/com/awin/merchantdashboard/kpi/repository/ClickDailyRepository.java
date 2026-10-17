package com.awin.merchantdashboard.kpi.repository;

import com.awin.merchantdashboard.kpi.model.ClickDaily;
import com.awin.merchantdashboard.kpi.model.ClickDailyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClickDailyRepository extends JpaRepository<ClickDaily, ClickDailyId> {}
