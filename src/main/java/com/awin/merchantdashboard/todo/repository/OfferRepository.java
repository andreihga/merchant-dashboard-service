package com.awin.merchantdashboard.todo.repository;

import com.awin.merchantdashboard.todo.model.Offer;
import com.awin.merchantdashboard.todo.model.OfferStatus;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer, Long> {

    long countByMerchantIdAndStatusAndExpiresAtBefore(long merchantId, OfferStatus status, Instant before);
}
