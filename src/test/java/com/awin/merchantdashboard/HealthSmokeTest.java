package com.awin.merchantdashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class HealthSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataSource dataSource;

    @Test
    void healthIsUpWithoutAToken(CapturedOutput output) throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));

        assertThat(output).containsPattern("\"traceId\"\\s*:\\s*\"[0-9a-fA-F]+\"");
        assertThat(output).doesNotContain("Using generated security password");
    }

    @Test
    void readinessIncludesTheDatabase() throws Exception {
        mockMvc.perform(get("/actuator/health/readiness"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
    }

    @Test
    void flywayRunsOnEmptyH2() throws Exception {
        try (Connection connection = dataSource.getConnection();
                Statement statement = connection.createStatement()) {
            try (ResultSet history =
                    statement.executeQuery("select count(*) from flyway_schema_history where version is not null")) {
                assertThat(history.next()).isTrue();
                assertThat(history.getInt(1)).isZero();
            }
            try (ResultSet created = statement.executeQuery("select count(*) from flyway_schema_history")) {
                assertThat(created.next()).isTrue();
                assertThat(created.getInt(1)).isPositive();
            }
            try (ResultSet merchant = statement.executeQuery(
                    "select count(*) from information_schema.tables where lower(table_name) = 'merchant'")) {
                assertThat(merchant.next()).isTrue();
                assertThat(merchant.getInt(1)).isZero();
            }
        }
    }
}
