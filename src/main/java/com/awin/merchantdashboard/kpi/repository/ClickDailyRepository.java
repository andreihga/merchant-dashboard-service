package com.awin.merchantdashboard.kpi.repository;

import com.awin.merchantdashboard.kpi.model.ClickDaily;
import com.awin.merchantdashboard.kpi.model.CountTotals;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClickDailyRepository extends JpaRepository<ClickDaily, Long> {

    /** Both windows are inclusive and the previous one ends before the current one starts. */
    @Query("""
            select new com.awin.merchantdashboard.kpi.model.CountTotals(
                coalesce(sum(case when c.clickDate >= :currentFrom then c.clickCount else 0 end), 0L),
                coalesce(sum(case when c.clickDate < :currentFrom then c.clickCount else 0 end), 0L))
            from ClickDaily c
            where c.merchantId = :merchantId
              and c.clickDate >= :previousFrom
              and c.clickDate <= :currentTo
            """)
    CountTotals sumClicks(
            @Param("merchantId") long merchantId,
            @Param("previousFrom") LocalDate previousFrom,
            @Param("currentFrom") LocalDate currentFrom,
            @Param("currentTo") LocalDate currentTo);
}
