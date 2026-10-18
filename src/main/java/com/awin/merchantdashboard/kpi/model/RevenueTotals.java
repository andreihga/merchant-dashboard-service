package com.awin.merchantdashboard.kpi.model;

import java.math.BigDecimal;

/** Sale amounts summed over the current and the previous window. */
public record RevenueTotals(BigDecimal current, BigDecimal previous) {}
