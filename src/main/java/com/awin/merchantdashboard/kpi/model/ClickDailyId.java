package com.awin.merchantdashboard.kpi.model;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class ClickDailyId implements Serializable {

    private Long merchantId;

    private LocalDate clickDate;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof ClickDailyId that)) {
            return false;
        }
        return Objects.equals(merchantId, that.merchantId) && Objects.equals(clickDate, that.clickDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(merchantId, clickDate);
    }
}
