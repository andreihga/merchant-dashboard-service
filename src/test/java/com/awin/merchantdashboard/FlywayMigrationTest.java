package com.awin.merchantdashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.instancio.Select.field;

import com.awin.merchantdashboard.auth.model.AppUser;
import com.awin.merchantdashboard.auth.repository.AppUserRepository;
import com.awin.merchantdashboard.kpi.model.ClickDaily;
import com.awin.merchantdashboard.kpi.model.SaleStatus;
import com.awin.merchantdashboard.kpi.model.SaleTransaction;
import com.awin.merchantdashboard.kpi.repository.ClickDailyRepository;
import com.awin.merchantdashboard.kpi.repository.SaleTransactionRepository;
import com.awin.merchantdashboard.merchant.model.Merchant;
import com.awin.merchantdashboard.merchant.model.UserMerchant;
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
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.Location;
import org.flywaydb.core.api.MigrationVersion;
import org.instancio.Instancio;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Autowired
    private Flyway flyway;

    @Autowired
    private MerchantRepository merchants;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private UserMerchantRepository userMerchants;

    @Autowired
    private SaleTransactionRepository sales;

    @Autowired
    private ClickDailyRepository clicks;

    @Autowired
    private OfferRepository offers;

    @Autowired
    private ProductCategoryRepository categories;

    @Autowired
    private AffiliateApplicationRepository applications;

    @Autowired
    private JdbcTemplate jdbc;

    // Runs after the insert so a row that escapes that transaction fails here.
    @Test
    @Order(2)
    void given_empty_h2_when_flyway_migrates_then_schema_exists_without_seed() throws Exception {
        assertThat(locations()).noneMatch(path -> path.contains("seed"));
        assertThat(flyway.info().pending()).isEmpty();
        List<String> expectedVersions = migrationVersionsOnClasspath();
        assertThat(expectedVersions).isNotEmpty();
        assertThat(appliedVersions()).containsExactlyElementsOf(expectedVersions);

        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            List<String> tables = schemaTablesExceptFlywayHistory(statement);
            assertThat(tables).isNotEmpty();
            for (String table : tables) {
                assertThat(table).matches("[a-z][a-z0-9_]*");
                try (ResultSet rows = statement.executeQuery("select count(*) from " + table)) {
                    assertThat(rows.next()).isTrue();
                    assertThat(rows.getLong(1)).as(table).isZero();
                }
            }
            try (ResultSet index = statement.executeQuery("""
                    select lower(column_name) from information_schema.index_columns
                    where lower(table_name) = 'sale_transaction'
                      and lower(index_name) = 'sale_transaction_pending_idx'
                    """)) {
                assertThat(index.next()).isTrue();
                assertThat(index.getString(1)).isEqualTo("merchant_id");
                assertThat(index.next()).isFalse();
            }
        }

        assertThat(merchants.count()).isZero();
        assertThat(users.count()).isZero();
        assertThat(userMerchants.count()).isZero();
        assertThat(sales.count()).isZero();
        assertThat(clicks.count()).isZero();
        assertThat(offers.count()).isZero();
        assertThat(categories.count()).isZero();
        assertThat(applications.count()).isZero();
    }

    @Test
    @Order(1)
    @Transactional
    void given_migrated_schema_when_persist_each_entity_then_rows_are_saved() {
        long merchantId = 1001L;
        merchants.saveAndFlush(Instancio.of(Merchant.class)
                .set(field(Merchant.class, "id"), merchantId)
                .set(field(Merchant.class, "name"), "Example Merchant")
                .set(field(Merchant.class, "currency"), "GBP")
                .create());
        users.saveAndFlush(Instancio.of(AppUser.class)
                .set(field(AppUser.class, "id"), null)
                .set(field(AppUser.class, "username"), "ada")
                .set(field(AppUser.class, "passwordHash"), "hash")
                .create());
        Long userId = jdbc.queryForObject("select id from app_user where username = ?", Long.class, "ada");

        userMerchants.saveAndFlush(Instancio.of(UserMerchant.class)
                .set(field(UserMerchant.class, "id"), null)
                .set(field(UserMerchant.class, "userId"), userId)
                .set(field(UserMerchant.class, "merchantId"), merchantId)
                .create());
        sales.saveAndFlush(Instancio.of(SaleTransaction.class)
                .set(field(SaleTransaction.class, "id"), null)
                .set(field(SaleTransaction.class, "merchantId"), merchantId)
                .set(field(SaleTransaction.class, "transactionDate"), Instant.parse("2026-01-15T10:00:00Z"))
                .set(field(SaleTransaction.class, "saleAmount"), new BigDecimal("10.5000"))
                .set(field(SaleTransaction.class, "status"), SaleStatus.PENDING)
                .create());
        clicks.saveAndFlush(Instancio.of(ClickDaily.class)
                .set(field(ClickDaily.class, "id"), null)
                .set(field(ClickDaily.class, "merchantId"), merchantId)
                .set(field(ClickDaily.class, "clickDate"), LocalDate.parse("2026-01-15"))
                .set(field(ClickDaily.class, "clickCount"), 12)
                .create());
        offers.saveAndFlush(Instancio.of(Offer.class)
                .set(field(Offer.class, "id"), null)
                .set(field(Offer.class, "merchantId"), merchantId)
                .set(field(Offer.class, "expiresAt"), Instant.parse("2026-02-01T00:00:00Z"))
                .set(field(Offer.class, "status"), OfferStatus.ACTIVE)
                .create());
        categories.saveAndFlush(Instancio.of(ProductCategory.class)
                .set(field(ProductCategory.class, "id"), null)
                .set(field(ProductCategory.class, "merchantId"), merchantId)
                .set(field(ProductCategory.class, "mapped"), false)
                .create());
        applications.saveAndFlush(Instancio.of(AffiliateApplication.class)
                .set(field(AffiliateApplication.class, "id"), null)
                .set(field(AffiliateApplication.class, "merchantId"), merchantId)
                .set(field(AffiliateApplication.class, "status"), ApplicationStatus.PENDING)
                .create());

        assertThat(jdbc.queryForObject("select name from merchant where id = ?", String.class, merchantId))
                .isEqualTo("Example Merchant");
        assertThat(jdbc.queryForObject(
                        "select count(*) from user_merchant where user_id = ? and merchant_id = ?",
                        Long.class,
                        userId,
                        merchantId))
                .isEqualTo(1L);
        assertThat(jdbc.queryForObject(
                        "select status from sale_transaction where merchant_id = ?", String.class, merchantId))
                .isEqualTo("PENDING");
        assertThat(jdbc.queryForObject(
                        "select click_count from click_daily where merchant_id = ?", Integer.class, merchantId))
                .isEqualTo(12);
        assertThat(jdbc.queryForObject("select status from offer where merchant_id = ?", String.class, merchantId))
                .isEqualTo("ACTIVE");
        assertThat(jdbc.queryForObject(
                        "select mapped from product_category where merchant_id = ?", Boolean.class, merchantId))
                .isFalse();
        assertThat(jdbc.queryForObject(
                        "select status from affiliate_application where merchant_id = ?", String.class, merchantId))
                .isEqualTo("PENDING");

        Long linkId = jdbc.queryForObject(
                "select id from user_merchant where user_id = ? and merchant_id = ?", Long.class, userId, merchantId);
        assertThat(linkId).isNotNull();
        assertThat(userMerchants.findById(linkId)).isPresent();
    }

    private List<String> locations() {
        List<String> paths = new ArrayList<>();
        for (Location location : flyway.getConfiguration().getLocations()) {
            paths.add(location.getRootPath());
        }
        return paths;
    }

    private List<String> migrationVersionsOnClasspath() throws IOException {
        Resource[] scripts =
                new PathMatchingResourcePatternResolver().getResources("classpath*:db/migration/V*__*.sql");
        List<String> versions = new ArrayList<>();
        for (Resource script : scripts) {
            String name = script.getFilename();
            int separator = name.indexOf("__");
            versions.add(name.substring(1, separator));
        }
        versions.sort(Comparator.comparing(MigrationVersion::fromVersion));
        return versions;
    }

    private List<String> schemaTablesExceptFlywayHistory(Statement statement) throws SQLException {
        List<String> tables = new ArrayList<>();
        try (ResultSet rows = statement.executeQuery("""
                select lower(table_name) from information_schema.tables
                where lower(table_schema) = 'public'
                  and table_type = 'BASE TABLE'
                  and lower(table_name) <> 'flyway_schema_history'
                order by table_name
                """)) {
            while (rows.next()) {
                tables.add(rows.getString(1));
            }
        }
        return tables;
    }

    private List<String> appliedVersions() throws Exception {
        List<String> versions = new ArrayList<>();
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement();
                ResultSet rows = statement.executeQuery(
                        "select version from flyway_schema_history where version is not null order by installed_rank")) {
            while (rows.next()) {
                versions.add(rows.getString(1));
            }
        }
        return versions;
    }
}
