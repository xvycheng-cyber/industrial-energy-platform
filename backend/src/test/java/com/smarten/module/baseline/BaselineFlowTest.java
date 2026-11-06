package com.smarten.module.baseline;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 动态基线端到端：训练、工况敏感、待机基线、版本切换。
 */
class BaselineFlowTest extends IntegrationTestSupport {

    private static final LocalDate START = LocalDate.of(2025, 3, 1);
    private static final LocalDate END = LocalDate.of(2025, 3, 10);
    private static final long DEVICE_ID = 3L;

    @Autowired
    private JdbcTemplate jdbc;

    @BeforeEach
    void seed() {
        cleanup();
        List<Object[]> production = new ArrayList<>();
        List<Object[]> aggregates = new ArrayList<>();
        for (LocalDate day = START; !day.isAfter(END); day = day.plusDays(1)) {
            for (int hour = 0; hour < 24; hour++) {
                boolean standby = hour < 6 || hour >= 22;
                double load = standby ? 0 : hour < 12 ? 45 : hour < 18 ? 75 : 90;
                double productBonus = day.getDayOfYear() % 2 == 0 ? 0 : 1.5;
                double output = standby ? 0 : 20 + load * 0.3;
                double energy = standby ? 1.2 : 2 + 0.25 * output + 0.06 * load + productBonus;
                int shift = hour < 8 ? 3 : hour < 16 ? 1 : 2;
                LocalDateTime time = day.atTime(hour, 0);

                production.add(new Object[]{
                        1L, 1L, 2L, DEVICE_ID, day, shift, time, time.plusHours(1),
                        productBonus > 0 ? "A型壳体" : "B型支架", output, "件",
                        standby ? 0 : 60, load, 22.0, standby ? 2 : 1
                });
                aggregates.add(new Object[]{
                        1L, 4, DEVICE_ID, 1L, 1, time, energy
                });
            }
        }

        jdbc.batchUpdate("""
                INSERT INTO production_record (enterprise_id, workshop_id, line_id, device_id, record_date, shift,
                                               start_time, end_time, product_type, output_qty, output_unit,
                                               run_minutes, load_rate, ambient_temp, status, data_source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'TEST')
                """, production);

        jdbc.batchUpdate("""
                INSERT INTO energy_aggregate (enterprise_id, dim_type, dim_id, energy_type_id, period_type,
                                              period_start, energy_value, cost_amount, data_count, missing_count,
                                              calc_version, deleted)
                VALUES (?, ?, ?, ?, ?, ?, ?, 0, 6, 0, 'TEST', 0)
                """, aggregates);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM production_record WHERE device_id = ? AND record_date BETWEEN ? AND ?",
                DEVICE_ID, START, END);
        jdbc.update("""
                DELETE FROM energy_aggregate
                WHERE dim_type = 4 AND dim_id = ? AND period_type = 1
                  AND period_start >= ? AND period_start < ?
                """, DEVICE_ID, START.atStartOfDay(), END.plusDays(1).atStartOfDay());
        jdbc.update("DELETE FROM energy_baseline_detail WHERE dim_type = 4 AND dim_id = ?", DEVICE_ID);
        jdbc.update("DELETE FROM energy_baseline WHERE dim_type = 4 AND dim_id = ?", DEVICE_ID);
    }

    private Map<String, Object> trainBody(boolean activate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("dimType", 4);
        body.put("dimId", DEVICE_ID);
        body.put("energyTypeId", 1);
        body.put("startDate", START.toString());
        body.put("endDate", END.toString());
        body.put("activate", activate);
        return body;
    }

    private JsonNode train(String token, boolean activate) throws Exception {
        String response = mockMvc.perform(post("/baseline/models/train")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(trainBody(activate))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data");
    }

    @Test
    @DisplayName("回归训练成功，不同负荷得到不同基线")
    void trainAndConditionSensitive() throws Exception {
        String token = login("admin", "Admin@123");
        JsonNode result = train(token, true);

        assertThat(result.path("algo").asText()).isEqualTo("RIDGE");
        assertThat(result.path("sampleCount").asInt()).isGreaterThan(100);
        assertThat(result.path("mae").asDouble()).isLessThan(5);
        assertThat(result.path("r2").asDouble()).isGreaterThan(0.8);
        assertThat(result.path("predictedCount").asInt()).isGreaterThan(100);

        long modelId = result.path("modelId").asLong();
        JsonNode records = objectMapper.readTree(mockMvc.perform(get("/baseline/records")
                        .header("Authorization", bearer(token))
                        .param("baselineId", String.valueOf(modelId))
                        .param("pageNum", "1")
                        .param("pageSize", "400"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data").path("records");

        double low = 0;
        double high = 0;
        double standby = 0;
        for (JsonNode item : records) {
            double load = item.path("condition").path("loadRate").asDouble();
            int type = item.path("baselineType").asInt();
            if (type == 1 && load > 40 && load < 50) {
                low = item.path("baselineValue").asDouble();
            }
            if (type == 1 && load > 85) {
                high = item.path("baselineValue").asDouble();
            }
            if (type == 5) {
                standby = item.path("baselineValue").asDouble();
            }
        }
        assertThat(low).isGreaterThan(0);
        assertThat(high).isGreaterThan(low + 3);
        assertThat(standby).isLessThan(low);
    }

    @Test
    @DisplayName("重复训练保留旧版本并支持版本切换")
    void versionSwitch() throws Exception {
        String token = login("admin", "Admin@123");
        JsonNode first = train(token, true);
        long firstId = first.path("modelId").asLong();
        JsonNode second = train(token, true);
        long secondId = second.path("modelId").asLong();

        assertThat(secondId).isNotEqualTo(firstId);
        assertThat(second.path("modelVersion").asText()).isNotEqualTo(first.path("modelVersion").asText());

        JsonNode models = objectMapper.readTree(mockMvc.perform(get("/baseline/models")
                        .header("Authorization", bearer(token))
                        .param("dimType", "4")
                        .param("dimId", String.valueOf(DEVICE_ID))
                        .param("pageNum", "1")
                        .param("pageSize", "20"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data").path("records");

        int firstStatus = -1;
        int activeCount = 0;
        for (JsonNode item : models) {
            if (item.path("id").asLong() == firstId) {
                firstStatus = item.path("status").asInt();
            }
            if (item.path("status").asInt() == 1) {
                activeCount++;
            }
        }
        assertThat(firstStatus).isEqualTo(2);
        assertThat(activeCount).isEqualTo(1);

        mockMvc.perform(post("/baseline/models/" + firstId + "/enable").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        JsonNode after = objectMapper.readTree(mockMvc.perform(get("/baseline/models/" + firstId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data");
        assertThat(after.path("status").asInt()).isEqualTo(1);
    }
}
