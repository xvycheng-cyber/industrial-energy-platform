package com.smarten.module.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarten.module.demo.service.DemoDataService;
import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 全链路联调：组织 → 采集 → DWD/DWS → 基线 → 诊断 → 整改 → 核算；并验证幂等与 Web/小程序同源。
 */
class FullChainFlowTest extends IntegrationTestSupport {

    @Autowired
    private DemoDataService demoDataService;

    @Autowired
    private JdbcTemplate jdbc;

    @AfterEach
    void cleanup() {
        demoDataService.reset();
        jdbc.update("DELETE FROM energy_meter WHERE meter_code LIKE 'DEMO-M-%'");
        jdbc.update("DELETE FROM energy_device WHERE code LIKE 'DEMO-DEV%'");
        jdbc.update("DELETE FROM energy_line WHERE code LIKE 'DEMO-LN%'");
        jdbc.update("DELETE FROM energy_workshop WHERE code LIKE 'DEMO-WS%'");
        jdbc.update("DELETE FROM energy_enterprise WHERE code = 'DEMO-FULL'");
        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'demo_engineer')");
        jdbc.update("DELETE FROM sys_user WHERE username = 'demo_engineer'");
    }

    private JsonNode runChain(String token) throws Exception {
        String response = mockMvc.perform(post("/demo/full-chain").param("days", "10")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data");
    }

    private JsonNode fetchStatus(String token) throws Exception {
        String response = mockMvc.perform(get("/demo/status").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data");
    }

    @Test
    @DisplayName("全链路：组织/四类能源/30天数据/基线/诊断/整改/核算，且幂等、Web与小程序同源")
    void fullChain() throws Exception {
        String admin = login("admin", "Admin@123");
        JsonNode first = runChain(admin);
        assertThat(first.path("workshops").asInt()).isEqualTo(3);
        assertThat(first.path("lines").asInt()).isEqualTo(6);
        assertThat(first.path("devices").asInt()).isEqualTo(30);
        assertThat(first.path("meters").asInt()).isEqualTo(40);
        assertThat(first.path("readings").asInt()).isGreaterThan(9000);
        assertThat(first.path("baselines").asInt()).isEqualTo(30);
        assertThat(first.path("anomalies").asInt()).isGreaterThan(0);
        assertThat(first.path("tasks").asInt()).isGreaterThanOrEqualTo(1);
        assertThat(first.path("savingConclusion").asText()).contains(":");

        JsonNode statusData = fetchStatus(admin);
        long raw = statusData.path("raw").asLong();
        long production = statusData.path("production").asLong();
        assertThat(raw).isGreaterThan(9000);
        assertThat(production).isBetween(30L * 10 * 24 - 4, 30L * 10 * 24);
        assertThat(statusData.path("devices").asLong()).isEqualTo(30);

        JsonNode loginNode = objectMapper.readTree(mockMvc.perform(post("/mobile/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"demo_engineer\",\"password\":\"Engineer@123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
        String mobile = loginNode.path("data").path("accessToken").asText();
        mockMvc.perform(get("/mobile/tasks").param("pageNum", "1").param("pageSize", "10")
                        .header("Authorization", bearer(mobile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total", org.hamcrest.Matchers.greaterThanOrEqualTo(1)));

        JsonNode second = runChain(admin);
        assertThat(second.path("devices").asInt()).isEqualTo(30);
        assertThat(second.path("meters").asInt()).isEqualTo(40);
        JsonNode status2 = fetchStatus(admin);
        assertThat(status2.path("raw").asLong()).isEqualTo(raw);
        assertThat(status2.path("production").asLong()).isEqualTo(production);
    }
}
