package com.awin.merchantdashboard.trend;

import static com.awin.merchantdashboard.helpers.TestHelpers.bearer;
import static com.awin.merchantdashboard.helpers.TestHelpers.link;
import static com.awin.merchantdashboard.helpers.TestHelpers.merchant;
import static com.awin.merchantdashboard.helpers.TestHelpers.sale;
import static com.awin.merchantdashboard.helpers.TestHelpers.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.awin.merchantdashboard.auth.model.AppUser;
import com.awin.merchantdashboard.auth.repository.AppUserRepository;
import com.awin.merchantdashboard.kpi.model.SaleStatus;
import com.awin.merchantdashboard.kpi.repository.SaleTransactionRepository;
import com.awin.merchantdashboard.merchant.repository.MerchantRepository;
import com.awin.merchantdashboard.merchant.repository.UserMerchantRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Today is 2026-10-18, so a rolling window ends on 2026-10-17. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RevenueTrendApiTest {

    private static final long MERCHANT = 8001L;
    private static final String BASE = "/api/v1/merchants/";
    private static final Pattern TOTAL = Pattern.compile("\"total\":(-?\\d+\\.\\d+)");
    private static final Pattern DATE = Pattern.compile("\"date\":\"(\\d{4}-\\d{2}-\\d{2})\"");
    private static final Pattern VALUE = Pattern.compile("\"value\":(-?\\d+\\.\\d+)");

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(Instant.parse("2026-10-18T10:00:00Z"), ZoneOffset.UTC);
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtEncoder jwtEncoder;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private MerchantRepository merchants;

    @Autowired
    private UserMerchantRepository links;

    @Autowired
    private SaleTransactionRepository sales;

    private AppUser caller;

    @BeforeEach
    void callerWithOneMerchant() {
        caller = users.saveAndFlush(user("trend-caller"));
        merchants.saveAndFlush(merchant(MERCHANT, "Trend Shop"));
        links.saveAndFlush(link(caller.getId(), MERCHANT));
    }

    @Test
    void given_sales_across_windows_when_get_revenue_trend_for_last_7_days_then_every_day_present_missing_day_zero_and_total_equals_current()
            throws Exception {
        // Current is 10-11..10-17. Previous is 10-04..10-10.
        saveSale(MERCHANT, "2026-10-11T00:00:00Z", "10.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-11T12:00:00Z", "5.5000", SaleStatus.PENDING);
        saveSale(MERCHANT, "2026-10-11T15:00:00Z", "100.0000", SaleStatus.DECLINED);
        saveSale(MERCHANT, "2026-10-15T09:00:00Z", "20.0050", SaleStatus.APPROVED);
        // 400ms before midnight still belongs to that UTC day.
        saveSale(MERCHANT, "2026-10-17T23:59:59.600Z", "3.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-04T00:00:00Z", "1.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-10T23:59:59.600Z", "4.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-18T00:00:00Z", "999.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-03T12:00:00Z", "999.0000", SaleStatus.APPROVED);
        merchants.saveAndFlush(merchant(8002L, "Other Shop"));

        saveSale(8002L, "2026-10-15T09:00:00Z", "999.0000", SaleStatus.APPROVED);

        String body = trend(MERCHANT, "LAST_7_DAYS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("LAST_7_DAYS"))
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andExpect(jsonPath("$.changePercent").value(670.20))
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> current = valuesOf(body, "current");
        assertThat(datesOf(body, "current")).containsExactlyElementsOf(days("2026-10-11", "2026-10-17"));
        assertThat(current).containsExactly("15.50", "0.00", "0.00", "0.00", "20.01", "0.00", "3.00");
        assertThat(datesOf(body, "previous")).containsExactlyElementsOf(days("2026-10-04", "2026-10-10"));
        assertThat(valuesOf(body, "previous")).containsExactly("1.00", "0.00", "0.00", "0.00", "0.00", "0.00", "4.00");
        assertThat(totalOf(body)).isEqualByComparingTo(sum(current));
    }

    @Test
    void given_sale_in_september_when_get_revenue_trend_for_last_month_then_every_day_of_each_month_present() throws Exception {
        saveSale(MERCHANT, "2026-09-15T10:00:00Z", "12.0000", SaleStatus.APPROVED);

        String body = trend(MERCHANT, "LAST_MONTH")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.period").value("LAST_MONTH"))
                .andExpect(jsonPath("$.changePercent").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        List<String> september = days("2026-09-01", "2026-09-30");
        List<String> septemberValues = zeros(september.size());
        septemberValues.set(september.indexOf("2026-09-15"), "12.00");

        List<String> current = valuesOf(body, "current");
        assertThat(datesOf(body, "current")).containsExactlyElementsOf(september);
        assertThat(current).containsExactlyElementsOf(septemberValues);
        assertThat(datesOf(body, "previous")).containsExactlyElementsOf(days("2026-08-01", "2026-08-31"));
        assertThat(valuesOf(body, "previous")).containsExactlyElementsOf(zeros(31));
        assertThat(totalOf(body)).isEqualByComparingTo(sum(current));
    }

    @Test
    void given_no_token_when_get_revenue_trend_then_returns_401() throws Exception {
        mockMvc.perform(get(BASE + MERCHANT + "/revenue-trend").param("period", "LAST_7_DAYS"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void given_unlinked_merchant_when_get_revenue_trend_then_returns_403() throws Exception {
        merchants.saveAndFlush(merchant(8003L, "Foreign Shop"));

        trend(8003L, "LAST_7_DAYS").andExpect(status().isForbidden());
    }

    @Test
    void given_unknown_merchant_when_get_revenue_trend_then_returns_404() throws Exception {
        trend(8999L, "LAST_7_DAYS").andExpect(status().isNotFound());
    }

    @Test
    void given_unknown_period_when_get_revenue_trend_then_returns_400() throws Exception {
        mockMvc.perform(get(BASE + MERCHANT + "/revenue-trend")
                        .param("period", "LAST_CENTURY")
                        .header("Authorization", bearer(jwtEncoder, caller.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    private ResultActions trend(long merchantId, String period) throws Exception {
        return mockMvc.perform(get(BASE + merchantId + "/revenue-trend")
                .param("period", period)
                .header("Authorization", bearer(jwtEncoder, caller.getId())));
    }

    private void saveSale(long merchantId, String instant, String amount, SaleStatus status) {
        sales.saveAndFlush(sale(merchantId, Instant.parse(instant), amount, status));
    }

    private static List<String> days(String from, String to) {
        List<String> dates = new ArrayList<>();
        for (LocalDate day = LocalDate.parse(from); !day.isAfter(LocalDate.parse(to)); day = day.plusDays(1)) {
            dates.add(day.toString());
        }
        return dates;
    }

    private static List<String> zeros(int count) {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add("0.00");
        }
        return values;
    }

    private static BigDecimal totalOf(String body) {
        Matcher matcher = TOTAL.matcher(body);
        assertThat(matcher.find()).isTrue();
        return new BigDecimal(matcher.group(1));
    }

    private static List<String> datesOf(String body, String series) {
        return matches(arrayOf(body, series), DATE);
    }

    private static List<String> valuesOf(String body, String series) {
        return matches(arrayOf(body, series), VALUE);
    }

    private static BigDecimal sum(List<String> values) {
        BigDecimal total = BigDecimal.ZERO;
        for (String value : values) {
            total = total.add(new BigDecimal(value));
        }
        return total;
    }

    private static String arrayOf(String body, String series) {
        int start = body.indexOf("\"" + series + "\":");
        int open = body.indexOf('[', start);
        int close = body.indexOf(']', open);
        return body.substring(open, close + 1);
    }

    private static List<String> matches(String array, Pattern pattern) {
        Matcher matcher = pattern.matcher(array);
        List<String> found = new ArrayList<>();
        while (matcher.find()) {
            found.add(matcher.group(1));
        }
        return found;
    }
}
