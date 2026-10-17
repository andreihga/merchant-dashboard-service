package com.awin.merchantdashboard.merchant.model;

import java.io.Serializable;
import java.util.Objects;

public class UserMerchantId implements Serializable {

    private Long userId;

    private Long merchantId;

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof UserMerchantId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(merchantId, that.merchantId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, merchantId);
    }
}
