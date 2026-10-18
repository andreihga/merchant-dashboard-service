package com.awin.merchantdashboard.kpi.repository;

import com.awin.merchantdashboard.kpi.model.CountTotals;
import com.awin.merchantdashboard.kpi.model.RevenueTotals;
import com.awin.merchantdashboard.kpi.model.SaleStatus;
import com.awin.merchantdashboard.kpi.model.SaleTransaction;
import java.time.Instant;
import java.util.Collection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * A window is {@code [from, to)}. The previous window must end where the current one starts or
 * before, so one pass over {@code [previousFrom, currentTo)} covers both.
 */
public interface SaleTransactionRepository extends JpaRepository<SaleTransaction, Long> {

    @Query("""
            select new com.awin.merchantdashboard.kpi.model.RevenueTotals(
                coalesce(sum(case when t.transactionDate >= :currentFrom then t.saleAmount else 0 end), 0),
                coalesce(sum(case when t.transactionDate < :currentFrom then t.saleAmount else 0 end), 0))
            from SaleTransaction t
            where t.merchantId = :merchantId
              and t.status in :statuses
              and t.transactionDate >= :previousFrom
              and t.transactionDate < :currentTo
            """)
    RevenueTotals sumRevenue(
            @Param("merchantId") long merchantId,
            @Param("statuses") Collection<SaleStatus> statuses,
            @Param("previousFrom") Instant previousFrom,
            @Param("currentFrom") Instant currentFrom,
            @Param("currentTo") Instant currentTo);

    @Query("""
            select new com.awin.merchantdashboard.kpi.model.CountTotals(
                coalesce(sum(case when t.transactionDate >= :currentFrom then 1L else 0L end), 0L),
                coalesce(sum(case when t.transactionDate < :currentFrom then 1L else 0L end), 0L))
            from SaleTransaction t
            where t.merchantId = :merchantId
              and t.status in :statuses
              and t.transactionDate >= :previousFrom
              and t.transactionDate < :currentTo
            """)
    CountTotals countSales(
            @Param("merchantId") long merchantId,
            @Param("statuses") Collection<SaleStatus> statuses,
            @Param("previousFrom") Instant previousFrom,
            @Param("currentFrom") Instant currentFrom,
            @Param("currentTo") Instant currentTo);
}
