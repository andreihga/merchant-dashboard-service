package com.awin.merchantdashboard.merchant.repository;

import com.awin.merchantdashboard.merchant.model.Merchant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MerchantRepository extends JpaRepository<Merchant, Long> {

    @Query("""
            select m from Merchant m
            where m.id in (select um.merchantId from UserMerchant um where um.userId = :userId)
            order by m.name, m.id
            """)
    List<Merchant> findAccessibleByUser(@Param("userId") long userId);
}
