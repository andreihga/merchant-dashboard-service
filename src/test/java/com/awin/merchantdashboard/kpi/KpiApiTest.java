package com.awin.merchantdashboard.kpi;

import static com.awin.merchantdashboard.helpers.TestHelpers.bearer;
import static com.awin.merchantdashboard.helpers.TestHelpers.link;
import static com.awin.merchantdashboard.helpers.TestHelpers.merchant;
import static com.awin.merchantdashboard.helpers.TestHelpers.sale;
import static com.awin.merchantdashboard.helpers.TestHelpers.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.awin.merchantdashboard.auth.model.AppUser;
import com.awin.merchantdashboard.auth.repository.AppUserRepository;
import com.awin.merchantdashboard.kpi.model.ClickDaily;
import com.awin.merchantdashboard.kpi.model.SaleStatus;
import com.awin.merchantdashboard.kpi.repository.ClickDailyRepository;
import com.awin.merchantdashboard.kpi.repository.SaleTransactionRepository;
import com.awin.merchantdashboard.merchant.repository.MerchantRepository;
import com.awin.merchantdashboard.merchant.repository.UserMerchantRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

/** Today is 2026-10-18, so every window ends on 2026-10-17. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class KpiApiTest {

    private static final long MERCHANT = 7001L;
    private static final String BASE = "/api/v1/merchants/";

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

    @Autowired
    private ClickDailyRepository clickRows;

    private AppUser caller;

    @BeforeEach
    void callerWithOneMerchant() {
        caller = users.saveAndFlush(user("kpi-caller"));
        merchants.saveAndFlush(merchant(MERCHANT, "Kpi Shop"));
        links.saveAndFlush(link(caller.getId(), MERCHANT));
    }

    @Test
    void
            given_sales_in_different_windows_when_get_revenue_and_transactions_with_different_periods_then_each_uses_its_own_window()
                    throws Exception {
        // 7-day window is 10-11..10-17 and its previous window is 10-04..10-10.
        saveSale(MERCHANT, "2026-10-15T09:00:00Z", "100.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-08T09:00:00Z", "50.0000", SaleStatus.APPROVED);
        // Only the 90-day window (07-20..10-17) reaches this one.
        saveSale(MERCHANT, "2026-08-01T09:00:00Z", "25.0000", SaleStatus.PENDING);

        kpi("revenue", "LAST_7_DAYS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric").value("REVENUE"))
                .andExpect(jsonPath("$.period").value("LAST_7_DAYS"))
                .andExpect(jsonPath("$.from").value("2026-10-11"))
                .andExpect(jsonPath("$.to").value("2026-10-17"))
                .andExpect(jsonPath("$.value").value(100.00))
                .andExpect(jsonPath("$.previousValue").value(50.00))
                .andExpect(jsonPath("$.changePercent").value(100.00));

        kpi("transactions", "LAST_90_DAYS")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.metric").value("TRANSACTIONS"))
                .andExpect(jsonPath("$.period").value("LAST_90_DAYS"))
                .andExpect(jsonPath("$.from").value("2026-07-20"))
                .andExpect(jsonPath("$.to").value("2026-10-17"))
                .andExpect(jsonPath("$.value").value(3))
                .andExpect(jsonPath("$.previousValue").value(0))
                .andExpect(jsonPath("$.changePercent").doesNotExist());
    }

    @Test
    void given_declined_sale_when_get_revenue_and_transactions_then_declined_is_excluded() throws Exception {
        saveSale(MERCHANT, "2026-10-15T09:00:00Z", "40.0000", SaleStatus.PENDING);
        saveSale(MERCHANT, "2026-10-15T10:00:00Z", "60.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-15T11:00:00Z", "900.0000", SaleStatus.DECLINED);
        saveSale(MERCHANT, "2026-10-08T11:00:00Z", "700.0000", SaleStatus.DECLINED);

        kpi("revenue", "LAST_7_DAYS")
                .andExpect(jsonPath("$.value").value(100.00))
                .andExpect(jsonPath("$.previousValue").value(0.00));
        kpi("transactions", "LAST_7_DAYS")
                .andExpect(jsonPath("$.value").value(2))
                .andExpect(jsonPath("$.previousValue").value(0));
    }

    @Test
    void given_fractional_sale_when_get_revenue_then_currency_scale_two_and_rounds_half_up() throws Exception {
        saveSale(MERCHANT, "2026-10-15T09:00:00Z", "10.0050", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-16T09:00:00Z", "20.0000", SaleStatus.APPROVED);

        String body = kpi("revenue", "LAST_7_DAYS")
                .andExpect(jsonPath("$.currency").value("GBP"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(body).contains("\"value\":30.01").contains("\"previousValue\":0.00");
    }

    @Test
    void given_sales_and_clicks_when_get_transactions_and_clicks_then_integers_with_no_currency() throws Exception {
        saveSale(MERCHANT, "2026-10-15T09:00:00Z", "10.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-16T09:00:00Z", "20.0000", SaleStatus.APPROVED);
        clickRows.saveAndFlush(clicks(MERCHANT, LocalDate.parse("2026-10-15"), 7));

        String transactions = kpi("transactions", "LAST_7_DAYS")
                .andExpect(jsonPath("$.currency").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String clickBody = kpi("clicks", "LAST_7_DAYS")
                .andExpect(jsonPath("$.currency").doesNotExist())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(transactions).contains("\"value\":2,").contains("\"previousValue\":0");
        assertThat(clickBody).contains("\"value\":7,").contains("\"previousValue\":0");
    }

    @Test
    void given_clicks_in_current_and_previous_windows_when_get_clicks_then_sums_window_and_compares_to_previous()
            throws Exception {
        clickRows.saveAndFlush(clicks(MERCHANT, LocalDate.parse("2026-10-17"), 10));
        clickRows.saveAndFlush(clicks(MERCHANT, LocalDate.parse("2026-10-11"), 20));
        clickRows.saveAndFlush(clicks(MERCHANT, LocalDate.parse("2026-10-10"), 15));
        clickRows.saveAndFlush(clicks(MERCHANT, LocalDate.parse("2026-10-04"), 15));
        // Today and the day before the previous window are outside both windows.
        clickRows.saveAndFlush(clicks(MERCHANT, LocalDate.parse("2026-10-18"), 1000));
        clickRows.saveAndFlush(clicks(MERCHANT, LocalDate.parse("2026-10-03"), 1000));

        kpi("clicks", "LAST_7_DAYS")
                .andExpect(jsonPath("$.metric").value("CLICKS"))
                .andExpect(jsonPath("$.from").value("2026-10-11"))
                .andExpect(jsonPath("$.to").value("2026-10-17"))
                .andExpect(jsonPath("$.value").value(30))
                .andExpect(jsonPath("$.previousValue").value(30))
                .andExpect(jsonPath("$.changePercent").value(0.00));
    }

    @Test
    void given_sales_at_utc_day_edges_when_get_transactions_then_counted_by_utc_day() throws Exception {
        saveSale(MERCHANT, "2026-10-17T23:59:59Z", "1.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-11T00:00:00Z", "2.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-10T23:59:59Z", "4.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-18T00:00:00Z", "100.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-03T23:59:59Z", "200.0000", SaleStatus.APPROVED);

        kpi("revenue", "LAST_7_DAYS")
                .andExpect(jsonPath("$.value").value(3.00))
                .andExpect(jsonPath("$.previousValue").value(4.00))
                .andExpect(jsonPath("$.changePercent").value(-25.00));
    }

    @Test
    void given_sales_in_previous_two_months_when_get_revenue_for_last_month_then_compared_with_month_before()
            throws Exception {
        saveSale(MERCHANT, "2026-09-30T23:00:00Z", "30.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-08-31T23:00:00Z", "10.0000", SaleStatus.APPROVED);
        saveSale(MERCHANT, "2026-10-02T09:00:00Z", "500.0000", SaleStatus.APPROVED);

        kpi("revenue", "LAST_MONTH")
                .andExpect(jsonPath("$.from").value("2026-09-01"))
                .andExpect(jsonPath("$.to").value("2026-09-30"))
                .andExpect(jsonPath("$.value").value(30.00))
                .andExpect(jsonPath("$.previousValue").value(10.00))
                .andExpect(jsonPath("$.changePercent").value(200.00));
    }

    @Test
    void given_other_merchant_sales_when_get_revenue_then_other_merchant_data_is_not_counted() throws Exception {
        merchants.saveAndFlush(merchant(7002L, "Other Shop"));
        saveSale(MERCHANT, "2026-10-15T09:00:00Z", "10.0000", SaleStatus.APPROVED);
        saveSale(7002L, "2026-10-15T09:00:00Z", "999.0000", SaleStatus.APPROVED);
        clickRows.saveAndFlush(clicks(MERCHANT, LocalDate.parse("2026-10-15"), 3));
        clickRows.saveAndFlush(clicks(7002L, LocalDate.parse("2026-10-15"), 999));

        kpi("revenue", "LAST_7_DAYS").andExpect(jsonPath("$.value").value(10.00));
        kpi("transactions", "LAST_7_DAYS").andExpect(jsonPath("$.value").value(1));
        kpi("clicks", "LAST_7_DAYS").andExpect(jsonPath("$.value").value(3));
    }

    @ParameterizedTest
    @ValueSource(strings = {"revenue", "transactions", "clicks"})
    void given_no_token_when_get_kpi_then_returns_401(String kpi) throws Exception {
        mockMvc.perform(get(BASE + MERCHANT + "/kpis/" + kpi).param("period", "LAST_7_DAYS"))
                .andExpect(status().isUnauthorized());
    }

    @ParameterizedTest
    @ValueSource(strings = {"revenue", "transactions", "clicks"})
    void given_unlinked_merchant_when_get_kpi_then_returns_403(String kpi) throws Exception {
        merchants.saveAndFlush(merchant(7003L, "Foreign Shop"));

        mockMvc.perform(get(BASE + 7003L + "/kpis/" + kpi)
                        .param("period", "LAST_7_DAYS")
                        .header("Authorization", bearer(jwtEncoder, caller.getId())))
                .andExpect(status().isForbidden());
    }

    @ParameterizedTest
    @ValueSource(strings = {"revenue", "transactions", "clicks"})
    void given_unknown_merchant_when_get_kpi_then_returns_404(String kpi) throws Exception {
        mockMvc.perform(get(BASE + 7999L + "/kpis/" + kpi)
                        .param("period", "LAST_7_DAYS")
                        .header("Authorization", bearer(jwtEncoder, caller.getId())))
                .andExpect(status().isNotFound());
    }

    @ParameterizedTest
    @ValueSource(strings = {"revenue", "transactions", "clicks"})
    void given_unknown_period_when_get_kpi_then_returns_400(String kpi) throws Exception {
        mockMvc.perform(get(BASE + MERCHANT + "/kpis/" + kpi)
                        .param("period", "LAST_CENTURY")
                        .header("Authorization", bearer(jwtEncoder, caller.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_PROBLEM_JSON_VALUE));
    }

    private ResultActions kpi(String kpi, String period) throws Exception {
        return mockMvc.perform(get(BASE + MERCHANT + "/kpis/" + kpi)
                .param("period", period)
                .header("Authorization", bearer(jwtEncoder, caller.getId())));
    }

    private void saveSale(long merchantId, String instant, String amount, SaleStatus status) {
        sales.saveAndFlush(sale(merchantId, Instant.parse(instant), amount, status));
    }

    private static ClickDaily clicks(long merchantId, LocalDate date, int count) {
        return Instancio.of(ClickDaily.class)
                .set(field(ClickDaily.class, "id"), null)
                .set(field(ClickDaily.class, "merchantId"), merchantId)
                .set(field(ClickDaily.class, "clickDate"), date)
                .set(field(ClickDaily.class, "clickCount"), count)
                .create();
    }
}
