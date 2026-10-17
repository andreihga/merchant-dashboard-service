package com.awin.merchantdashboard.merchant.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "user_merchant")
@IdClass(UserMerchantId.class)
public class UserMerchant {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @Id
    @Column(name = "merchant_id")
    private Long merchantId;
}
