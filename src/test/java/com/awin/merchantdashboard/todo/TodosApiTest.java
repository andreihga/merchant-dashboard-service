package com.awin.merchantdashboard.todo;

import static com.awin.merchantdashboard.helpers.TestHelpers.bearer;
import static com.awin.merchantdashboard.helpers.TestHelpers.link;
import static com.awin.merchantdashboard.helpers.TestHelpers.merchant;
import static com.awin.merchantdashboard.helpers.TestHelpers.sale;
import static com.awin.merchantdashboard.helpers.TestHelpers.user;
import static org.instancio.Select.field;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.awin.merchantdashboard.auth.model.AppUser;
import com.awin.merchantdashboard.auth.repository.AppUserRepository;
import com.awin.merchantdashboard.kpi.model.SaleStatus;
import com.awin.merchantdashboard.kpi.repository.SaleTransactionRepository;
import com.awin.merchantdashboard.merchant.repository.MerchantRepository;
import com.awin.merchantdashboard.merchant.repository.UserMerchantRepository;
import com.awin.merchantdashboard.todo.model.AffiliateApplication;
import com.awin.merchantdashboard.todo.model.ApplicationStatus;
import com.awin.merchantdashboard.todo.model.Offer;
import com.awin.merchantdashboard.todo.model.OfferStatus;
import com.awin.merchantdashboard.todo.model.ProductCategory;
import com.awin.merchantdashboard.todo.repository.AffiliateApplicationRepository;
import com.awin.merchantdashboard.todo.repository.OfferRepository;
import com.awin.merchantdashboard.todo.repository.ProductCategoryRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.instancio.Instancio;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

/** Now is 2026-10-18T10:00:00Z. Expired offers are those with expires_at before that instant. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class TodosApiTest {

    private static final long MERCHANT = 9001L;
    private static final String BASE = "/api/v1/merchants/";
    private static final Instant NOW = Instant.parse("2026-10-18T10:00:00Z");

    @TestConfiguration
    static class FixedClock {
        @Bean
        @Primary
        Clock fixedClock() {
            return Clock.fixed(NOW, ZoneOffset.UTC);
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
    private OfferRepository offers;

    @Autowired
    private ProductCategoryRepository categories;

    @Autowired
    private SaleTransactionRepository sales;

    @Autowired
    private AffiliateApplicationRepository applications;

    private AppUser caller;

    @BeforeEach
    void callerWithOneMerchant() {
        caller = users.saveAndFlush(user("todo-caller"));
        merchants.saveAndFlush(merchant(MERCHANT, "Todo Shop"));
        links.saveAndFlush(link(caller.getId(), MERCHANT));
    }

    @Test
    void given_mixed_todo_rows_when_get_todos_then_returns_four_counts() throws Exception {
        offers.saveAndFlush(offer(MERCHANT, Instant.parse("2026-10-17T00:00:00Z"), OfferStatus.ACTIVE));
        offers.saveAndFlush(offer(MERCHANT, Instant.parse("2026-10-01T00:00:00Z"), OfferStatus.ACTIVE));
        offers.saveAndFlush(offer(MERCHANT, Instant.parse("2026-10-17T00:00:00Z"), OfferStatus.REMOVED));
        offers.saveAndFlush(offer(MERCHANT, Instant.parse("2026-10-19T00:00:00Z"), OfferStatus.ACTIVE));

        categories.saveAndFlush(category(MERCHANT, false));
        categories.saveAndFlush(category(MERCHANT, false));
        categories.saveAndFlush(category(MERCHANT, false));
        categories.saveAndFlush(category(MERCHANT, true));

        sales.saveAndFlush(sale(MERCHANT, Instant.parse("2026-10-15T09:00:00Z"), "10.0000", SaleStatus.PENDING));
        sales.saveAndFlush(sale(MERCHANT, Instant.parse("2026-10-16T09:00:00Z"), "20.0000", SaleStatus.PENDING));
        sales.saveAndFlush(sale(MERCHANT, Instant.parse("2026-10-16T10:00:00Z"), "30.0000", SaleStatus.APPROVED));
        sales.saveAndFlush(sale(MERCHANT, Instant.parse("2026-10-16T11:00:00Z"), "40.0000", SaleStatus.DECLINED));

        applications.saveAndFlush(application(MERCHANT, ApplicationStatus.PENDING));
        applications.saveAndFlush(application(MERCHANT, ApplicationStatus.PENDING));
        applications.saveAndFlush(application(MERCHANT, ApplicationStatus.PENDING));
        applications.saveAndFlush(application(MERCHANT, ApplicationStatus.PENDING));
        applications.saveAndFlush(application(MERCHANT, ApplicationStatus.REVIEWED));

        merchants.saveAndFlush(merchant(9002L, "Other Shop"));
        offers.saveAndFlush(offer(9002L, Instant.parse("2026-10-01T00:00:00Z"), OfferStatus.ACTIVE));
        categories.saveAndFlush(category(9002L, false));
        sales.saveAndFlush(sale(9002L, Instant.parse("2026-10-15T09:00:00Z"), "99.0000", SaleStatus.PENDING));
        applications.saveAndFlush(application(9002L, ApplicationStatus.PENDING));

        todos(MERCHANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiredOffers").value(2))
                .andExpect(jsonPath("$.categoriesToMap").value(3))
                .andExpect(jsonPath("$.commissionsToValidate").value(2))
                .andExpect(jsonPath("$.applicationsToReview").value(4))
                .andExpect(jsonPath("$.period").doesNotExist());
    }

    @Test
    void given_no_todo_rows_when_get_todos_then_returns_zeros() throws Exception {
        todos(MERCHANT)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.expiredOffers").value(0))
                .andExpect(jsonPath("$.categoriesToMap").value(0))
                .andExpect(jsonPath("$.commissionsToValidate").value(0))
                .andExpect(jsonPath("$.applicationsToReview").value(0));
    }

    @Test
    void given_no_token_when_get_todos_then_returns_401() throws Exception {
        mockMvc.perform(get(BASE + MERCHANT + "/todos")).andExpect(status().isUnauthorized());
    }

    @Test
    void given_unlinked_merchant_when_get_todos_then_returns_403() throws Exception {
        merchants.saveAndFlush(merchant(9003L, "Foreign Shop"));

        todos(9003L).andExpect(status().isForbidden());
    }

    @Test
    void given_unknown_merchant_when_get_todos_then_returns_404() throws Exception {
        todos(9999L).andExpect(status().isNotFound());
    }

    private ResultActions todos(long merchantId) throws Exception {
        return mockMvc.perform(
                get(BASE + merchantId + "/todos").header("Authorization", bearer(jwtEncoder, caller.getId())));
    }

    private static Offer offer(long merchantId, Instant expiresAt, OfferStatus status) {
        return Instancio.of(Offer.class)
                .set(field(Offer.class, "id"), null)
                .set(field(Offer.class, "merchantId"), merchantId)
                .set(field(Offer.class, "expiresAt"), expiresAt)
                .set(field(Offer.class, "status"), status)
                .create();
    }

    private static ProductCategory category(long merchantId, boolean mapped) {
        return Instancio.of(ProductCategory.class)
                .set(field(ProductCategory.class, "id"), null)
                .set(field(ProductCategory.class, "merchantId"), merchantId)
                .set(field(ProductCategory.class, "mapped"), mapped)
                .create();
    }

    private static AffiliateApplication application(long merchantId, ApplicationStatus status) {
        return Instancio.of(AffiliateApplication.class)
                .set(field(AffiliateApplication.class, "id"), null)
                .set(field(AffiliateApplication.class, "merchantId"), merchantId)
                .set(field(AffiliateApplication.class, "status"), status)
                .create();
    }
}
