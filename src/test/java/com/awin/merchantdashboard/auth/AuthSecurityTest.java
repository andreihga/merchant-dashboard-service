package com.awin.merchantdashboard.auth;

import static com.awin.merchantdashboard.helpers.TestHelpers.link;
import static com.awin.merchantdashboard.helpers.TestHelpers.merchant;
import static com.awin.merchantdashboard.helpers.TestHelpers.user;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.awin.merchantdashboard.auth.model.AppUser;
import com.awin.merchantdashboard.auth.repository.AppUserRepository;
import com.awin.merchantdashboard.merchant.repository.MerchantRepository;
import com.awin.merchantdashboard.merchant.repository.UserMerchantRepository;
import com.awin.merchantdashboard.merchant.service.MerchantAccessGuard;
import com.jayway.jsonpath.JsonPath;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Import(AuthSecurityTest.MerchantAccessProbe.class)
@ExtendWith(OutputCaptureExtension.class)
class AuthSecurityTest {

    private static final String USERNAME = "ada";
    private static final String PASSWORD = "correct-password";
    private static final String PASSWORD_HASH = new BCryptPasswordEncoder().encode(PASSWORD);
    private static final long LINKED_MERCHANT_ID = 5011L;
    private static final long FOREIGN_MERCHANT_ID = 5012L;
    private static final long UNKNOWN_MERCHANT_ID = 5013L;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private AppUserRepository users;

    @Autowired
    private MerchantRepository merchants;

    @Autowired
    private UserMerchantRepository links;

    private Long userId;

    @BeforeEach
    void insertUserAndMerchants() {
        merchants.saveAndFlush(merchant(LINKED_MERCHANT_ID, "Merchant " + LINKED_MERCHANT_ID));
        merchants.saveAndFlush(merchant(FOREIGN_MERCHANT_ID, "Merchant " + FOREIGN_MERCHANT_ID));
        AppUser user = users.saveAndFlush(user(USERNAME, PASSWORD_HASH));
        userId = user.getId();
        links.saveAndFlush(link(userId, LINKED_MERCHANT_ID));
    }

    @Test
    void given_wrong_password_when_post_auth_token_then_returns_401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(USERNAME, "wrong-password")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void given_unknown_user_when_post_auth_token_then_returns_401() throws Exception {
        mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials("nobody", PASSWORD)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void given_no_token_when_get_protected_resource_then_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/{merchantId}/access", LINKED_MERCHANT_ID))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void given_invalid_token_when_get_protected_resource_then_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/{merchantId}/access", LINKED_MERCHANT_ID)
                        .header("Authorization", "Bearer not-a-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void given_valid_credentials_when_post_auth_token_then_returns_jwt_for_user_expiring_in_one_hour(CapturedOutput output)
            throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
        Jwt jwt = jwtDecoder.decode(token);
        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getExpiresAt()).isEqualTo(jwt.getIssuedAt().plus(Duration.ofHours(1)));
        assertThat(output.getAll()).doesNotContain(PASSWORD, token);
    }

    @Test
    void given_linked_merchant_when_get_protected_resource_then_returns_200() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/{merchantId}/access", LINKED_MERCHANT_ID)
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isOk());
    }

    @Test
    void given_unlinked_merchant_when_get_protected_resource_then_returns_403() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/{merchantId}/access", FOREIGN_MERCHANT_ID)
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isForbidden());
    }

    @Test
    void given_unknown_merchant_when_get_protected_resource_then_returns_404() throws Exception {
        mockMvc.perform(get("/api/v1/merchants/{merchantId}/access", UNKNOWN_MERCHANT_ID)
                        .header("Authorization", "Bearer " + login()))
                .andExpect(status().isNotFound());
    }

    private String login() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(credentials(USERNAME, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn();
        return JsonPath.read(result.getResponse().getContentAsString(), "$.accessToken");
    }

    private static String credentials(String username, String password) {
        return "{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}";
    }

    @RestController
    static class MerchantAccessProbe {

        private final MerchantAccessGuard guard;

        MerchantAccessProbe(MerchantAccessGuard guard) {
            this.guard = guard;
        }

        @GetMapping("/api/v1/merchants/{merchantId}/access")
        ResponseEntity<Void> access(@PathVariable long merchantId, @AuthenticationPrincipal Jwt jwt) {
            guard.check(Long.parseLong(jwt.getSubject()), merchantId);
            return ResponseEntity.ok().build();
        }
    }
}
