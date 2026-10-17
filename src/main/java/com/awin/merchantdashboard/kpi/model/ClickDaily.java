package com.awin.merchantdashboard.kpi.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "click_daily")
@IdClass(ClickDailyId.class)
public class ClickDaily {

    @Id
    @Column(name = "merchant_id")
    private Long merchantId;

    @Id
    @Column(name = "click_date")
    private LocalDate clickDate;

    @Column(name = "click_count", nullable = false)
    private int clickCount;
}
