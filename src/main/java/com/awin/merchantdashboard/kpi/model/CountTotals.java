package com.awin.merchantdashboard.kpi.model;

/** A count summed over the current and the previous window. */
public record CountTotals(long current, long previous) {}
