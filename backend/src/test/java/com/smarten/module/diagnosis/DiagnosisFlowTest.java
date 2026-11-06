package com.smarten.module.diagnosis;

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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 异常诊断端到端：扫描→异常→报告→人工确认→处理→重复扫描合并。
 */
class DiagnosisFlowTest extends IntegrationTestSupport {

    private static final long DEVICE_ID = 4L;
    private static final LocalDate DAY = LocalDate.of(2025, 5, 1);
    private static final LocalDate DAY2 = LocalDate.of(2025, 5, 2);

    @Autowired
    private JdbcTemplate jdbc;

    private long baselineId;

    @BeforeEach
    void seed() {
        cleanup();
        jdbc.update("""
                INSERT INTO energy_baseline (enterprise_id, dim_type, dim_id, energy_type_id, granularity,
                                             baseline_type, model_version, algo, sample_count, status,
                                             params_json, feature_json, effective_from, calc_time, deleted)
                VALUES (1, 4, ?, 1, 3, 1, 'test-diag-v1', 'RIDGE', 48, 1, '{}', '[]', NOW(), NOW(), 0)
                """, DEVICE_ID);
        baselineId = jdbc.queryForObject("SELECT MAX(id) FROM energy_baseline WHERE dim_id = ?", Long.class, DEVICE_ID);

        List<Object[]> details = new ArrayList<>();
        List<Object[]> production = new ArrayList<>();
        for (LocalDate day : List.of(DAY, DAY2)) {
            for (int hour = 0; hour < 24; hour++) {
                LocalDateTime time = day.atTime(hour, 0);
                double baseline = 8 + 2 * Math.sin(hour / 24.0 * 2 * Math.PI);
                double actual = baseline;
                if (day.equals(DAY) && hour >= 14 && hour <= 16) {
                    actual = baseline * 1.8; // 连续 3 小时高能耗
                }
                boolean standby = hour < 6;
                details.add(new Object[]{
                        baselineId, 1L, 4, DEVICE_ID, 1L, time, time.plusHours(1),
                        baseline, actual, actual - baseline, (actual - baseline) / baseline * 100,
                        standby ? 5 : 1, "test-diag-v1", "{}", 0, 1, LocalDateTime.now()
                });
                production.add(new Object[]{
                        1L, 1L, 2L, DEVICE_ID, day, hour < 8 ? 3 : hour < 16 ? 1 : 2,
                        time, time.plusHours(1), "A型壳体", standby ? 0 : 30, "件",
                        standby ? 0 : 60, standby ? 0 : 70, standby ? 2 : 1
                });
            }
        }
        jdbc.batchUpdate("""
                INSERT INTO energy_baseline_detail (baseline_id, enterprise_id, dim_type, dim_id, energy_type_id,
                                                    period_start, period_end, baseline_value, actual_value,
                                                    deviation_value, deviation_rate, baseline_type, model_version,
                                                    condition_json, estimated, comparable, computed_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, details);
        jdbc.batchUpdate("""
                INSERT INTO production_record (enterprise_id, workshop_id, line_id, device_id, record_date, shift,
                                               start_time, end_time, product_type, output_qty, output_unit,
                                               run_minutes, load_rate, status, data_source)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 'TEST')
                """, production);
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE FROM diagnosis_report WHERE device_id = ?", DEVICE_ID);
        jdbc.update("DELETE FROM energy_anomaly WHERE device_id = ?", DEVICE_ID);
        jdbc.update("DELETE FROM energy_baseline_detail WHERE dim_id = ? AND dim_type = 4", DEVICE_ID);
        jdbc.update("DELETE FROM energy_baseline WHERE dim_id = ? AND dim_type = 4", DEVICE_ID);
        jdbc.update("DELETE FROM production_record WHERE device_id = ? AND record_date BETWEEN ? AND ?",
                DEVICE_ID, DAY, DAY2);
    }

    private JsonNode scan(String token, LocalDateTime start, LocalDateTime end) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("enterpriseId", 1);
        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        body.put("start", start.format(fmt));
        body.put("end", end.format(fmt));
        String response = mockMvc.perform(post("/diagnosis/scan")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode node = objectMapper.readTree(response);
        assertThat(node.path("code").asInt()).as(response).isEqualTo(200);
        return node.path("data");
    }

    @Test
    @DisplayName("扫描生成异常与报告，重复扫描合并而非重复创建")
    void scanCreatesAnomalyAndMerges() throws Exception {
        String token = login("admin", "Admin@123");
        JsonNode first = scan(token, DAY.atStartOfDay(), DAY2.plusDays(1).atStartOfDay());
        assertThat(first.path("newAnomalies").asInt()).isGreaterThan(0);
        assertThat(first.path("reportCount").asInt()).isGreaterThan(0);

        JsonNode list = objectMapper.readTree(mockMvc.perform(get("/diagnosis/anomalies")
                        .header("Authorization", bearer(token))
                        .param("deviceId", String.valueOf(DEVICE_ID))
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data").path("records");
        assertThat(list.size()).isGreaterThan(0);
        long anomalyId = list.get(0).path("id").asLong();
        assertThat(list.get(0).path("reportId").asLong()).isGreaterThan(0);
        assertThat(list.get(0).path("riskScore").asDouble()).isGreaterThan(0);

        JsonNode second = scan(token, DAY.atStartOfDay(), DAY2.plusDays(1).atStartOfDay());
        assertThat(second.path("mergedAnomalies").asInt()).isGreaterThan(0);

        JsonNode after = objectMapper.readTree(mockMvc.perform(get("/diagnosis/anomalies")
                        .header("Authorization", bearer(token))
                        .param("deviceId", String.valueOf(DEVICE_ID))
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data").path("records");
        assertThat(after.size()).isEqualTo(list.size());

        JsonNode report = objectMapper.readTree(mockMvc.perform(get("/diagnosis/anomalies/" + anomalyId + "/report")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data");
        assertThat(report.path("causes").size()).isGreaterThan(0);
        assertThat(report.path("suggestions").size()).isGreaterThan(0);
        assertThat(report.path("description").asText()).isNotBlank();
        assertThat(report.path("confirmedCause").isNull()).isTrue();

        long reportId = report.path("id").asLong();
        mockMvc.perform(post("/diagnosis/reports/" + reportId + "/confirm")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"confirmedCause\":\"润滑不良导致摩擦增大\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        JsonNode confirmed = objectMapper.readTree(mockMvc.perform(get("/diagnosis/reports/" + reportId)
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data");
        assertThat(confirmed.path("confirmedCause").asText()).contains("润滑");

        mockMvc.perform(post("/diagnosis/anomalies/" + anomalyId + "/handle")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":1,\"remark\":\"已派工\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/diagnosis/anomalies/" + anomalyId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.statusLabel").value("已确认"));
    }

    @Test
    @DisplayName("规则更新仅接受白名单字段")
    void ruleValidationRejectsUnknownField() throws Exception {
        String token = login("admin", "Admin@123");
        JsonNode rules = objectMapper.readTree(mockMvc.perform(get("/diagnosis/rules")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data");
        long ruleId = rules.get(0).path("id").asLong();

        mockMvc.perform(put("/diagnosis/rules/" + ruleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"condition\":{\"threshold\":40,\"sql\":\"drop table x\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(put("/diagnosis/rules/" + ruleId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"condition\":{\"threshold\":45},\"threshold\":{\"threshold\":45}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.version").isNumber());
    }

    @Test
    @DisplayName("总览返回趋势与分布")
    void overview() throws Exception {
        String token = login("admin", "Admin@123");
        scan(token, DAY.atStartOfDay(), DAY2.plusDays(1).atStartOfDay());
        JsonNode data = objectMapper.readTree(mockMvc.perform(get("/diagnosis/overview")
                        .header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8))
                .path("data");
        assertThat(data.path("total").asInt()).isGreaterThan(0);
        assertThat(data.path("trend").size()).isGreaterThan(0);
        assertThat(data.path("byRule").size()).isGreaterThan(0);
    }
}
