package com.awin.merchantdashboard.helpers;

import static org.instancio.Select.field;

import com.awin.merchantdashboard.auth.model.AppUser;
import com.awin.merchantdashboard.kpi.model.SaleStatus;
import com.awin.merchantdashboard.kpi.model.SaleTransaction;
import com.awin.merchantdashboard.merchant.model.Merchant;
import com.awin.merchantdashboard.merchant.model.UserMerchant;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import org.instancio.Instancio;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

/**
 * Shared helpers for API tests. The builders return unsaved entities, so the test decides when and
 * where to persist them. Tests that are not about login use {@link #bearer} instead of calling the
 * login endpoint, which {@code AuthSecurityTest} covers on its own.
 */
public final class TestHelpers {

    private static final Duration TOKEN_TTL = Duration.ofHours(1);

    private TestHelpers() {}

    /** An unsaved user with a generated password hash. Set the hash yourself if the test logs in. */
    public static AppUser user(String username) {
        return Instancio.of(AppUser.class)
                .set(field(AppUser.class, "id"), null)
                .set(field(AppUser.class, "username"), username)
                .create();
    }

    public static AppUser user(String username, String passwordHash) {
        return Instancio.of(AppUser.class)
                .set(field(AppUser.class, "id"), null)
                .set(field(AppUser.class, "username"), username)
                .set(field(AppUser.class, "passwordHash"), passwordHash)
                .create();
    }

    public static Merchant merchant(long id, String name) {
        return Instancio.of(Merchant.class)
                .set(field(Merchant.class, "id"), id)
                .set(field(Merchant.class, "name"), name)
                .set(field(Merchant.class, "currency"), "GBP")
                .create();
    }

    /** An unsaved sale. The id is null so the database assigns it. */
    public static SaleTransaction sale(long merchantId, Instant date, String amount, SaleStatus status) {
        return Instancio.of(SaleTransaction.class)
                .set(field(SaleTransaction.class, "id"), null)
                .set(field(SaleTransaction.class, "merchantId"), merchantId)
                .set(field(SaleTransaction.class, "transactionDate"), date)
                .set(field(SaleTransaction.class, "saleAmount"), new BigDecimal(amount))
                .set(field(SaleTransaction.class, "status"), status)
                .create();
    }

    public static UserMerchant link(long userId, long merchantId) {
        return Instancio.of(UserMerchant.class)
                .set(field(UserMerchant.class, "id"), null)
                .set(field(UserMerchant.class, "userId"), userId)
                .set(field(UserMerchant.class, "merchantId"), merchantId)
                .create();
    }

    /** The value for an {@code Authorization} header: a signed, one-hour JWT whose subject is the user id. */
    public static String bearer(JwtEncoder encoder, long userId) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(Long.toString(userId))
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(
                        JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return "Bearer " + token;
    }
}
