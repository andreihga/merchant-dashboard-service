package com.awin.merchantdashboard.merchant;

import static com.awin.merchantdashboard.helpers.TestHelpers.bearer;
import static com.awin.merchantdashboard.helpers.TestHelpers.link;
import static com.awin.merchantdashboard.helpers.TestHelpers.merchant;
import static com.awin.merchantdashboard.helpers.TestHelpers.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.awin.merchantdashboard.auth.model.AppUser;
import com.awin.merchantdashboard.auth.repository.AppUserRepository;
import com.awin.merchantdashboard.merchant.repository.MerchantRepository;
import com.awin.merchantdashboard.merchant.repository.UserMerchantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class MerchantsApiTest {

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

    @Test
    void given_linked_merchants_when_get_merchants_then_only_callers_merchants_ordered_by_name_then_id()
            throws Exception {
        AppUser caller = users.saveAndFlush(user("caller"));
        AppUser other = users.saveAndFlush(user("other"));
        merchants.saveAndFlush(merchant(6003L, "Beta Shop"));
        merchants.saveAndFlush(merchant(6002L, "Alpha Shop"));
        merchants.saveAndFlush(merchant(6001L, "Beta Shop"));
        merchants.saveAndFlush(merchant(6004L, "Foreign Shop"));
        links.saveAndFlush(link(caller.getId(), 6003L));
        links.saveAndFlush(link(caller.getId(), 6002L));
        links.saveAndFlush(link(caller.getId(), 6001L));
        links.saveAndFlush(link(other.getId(), 6004L));

        mockMvc.perform(get("/api/v1/merchants").header("Authorization", bearer(jwtEncoder, caller.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value(6002))
                .andExpect(jsonPath("$[0].name").value("Alpha Shop"))
                .andExpect(jsonPath("$[0].currency").value("GBP"))
                .andExpect(jsonPath("$[1].id").value(6001))
                .andExpect(jsonPath("$[2].id").value(6003));
    }

    @Test
    void given_user_with_no_linked_merchants_when_get_merchants_then_returns_empty_list() throws Exception {
        AppUser loner = users.saveAndFlush(user("loner"));
        merchants.saveAndFlush(merchant(6005L, "Unlinked Shop"));

        mockMvc.perform(get("/api/v1/merchants").header("Authorization", bearer(jwtEncoder, loner.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void given_no_token_when_get_merchants_then_returns_401() throws Exception {
        mockMvc.perform(get("/api/v1/merchants")).andExpect(status().isUnauthorized());
    }
}
