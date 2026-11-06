package com.smarten.module.saving;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 节能核算端到端：复算版本、审核完成、数据不足、导出与追溯。
 */
class SavingFlowTest extends IntegrationTestSupport {

    private static final long DEVICE_ID = 4L;
    private static final LocalDateTime START = LocalDateTime.of(2025, 6, 1, 0, 0);

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
                VALUES (1, 4, ?, 1, 3, 1, 'test-p8-v1', 'RIDGE', 100, 1, '{}', '[]', NOW(), NOW(), 0)
                """, DEVICE_ID);
        baselineId = jdbc.queryForObject("SELECT MAX(id) FROM energy_baseline WHERE dim_id = ?", Long.class, DEVICE_ID);

        List<Object[]> details = new ArrayList<>();
        List<Object[]> production = new ArrayList<>();
        for (int hour = 0; hour < 7 * 24; hour++) {
            LocalDateTime time = START.plusHours(hour);
            double baseline = 10;
            double actual = 8;
            details.add(new Object[]{
                    baselineId, 1L, 4, DEVICE_ID, 1L, time, time.plusHours(1),
                    baseline, actual, actual - baseline, (actual - baseline) / baseline * 100,
                    1, "test-p8-v1", "{}", 0, 1, LocalDateTime.now()
            });
            production.add(new Object[]{
                    1L, 1L, 2L, DEVICE_ID, time.toLocalDate(), hour % 24 < 8 ? 3 : hour % 24 < 16 ? 1 : 2,
                    time, time.plusHours(1), "A型壳体", 30, "件", 60, 70, 1
            });
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
        jdbc.update("DELETE FROM energy_saving_result WHERE device_id = ?", DEVICE_ID);
        jdbc.update("DELETE r FROM rectification_task r WHERE r.title LIKE 'TEST-P8%'");
        jdbc.update("DELETE FROM energy_baseline_detail WHERE dim_id = ? AND dim_type = 4", DEVICE_ID);
        jdbc.update("DELETE FROM energy_baseline WHERE dim_id = ? AND dim_type = 4", DEVICE_ID);
        jdbc.update("DELETE FROM production_record WHERE device_id = ? AND record_date BETWEEN ? AND ?",
                DEVICE_ID, LocalDate.of(2025, 6, 1), LocalDate.of(2025, 6, 8));
    }

    private long createTask(String suffix) {
        jdbc.update("""
                INSERT INTO rectification_task (task_no, enterprise_id, line_id, device_id, title, status,
                                                priority, expected_saving, progress, source, finish_time, audit_time, deleted)
                VALUES (?, 1, 2, ?, ?, 7, 2, 100, 100, 1, ?, ?, 0)
                """, "TEST-P8-" + suffix, DEVICE_ID, "TEST-P8-" + suffix, START, START);
        return jdbc.queryForObject("SELECT id FROM rectification_task WHERE task_no = ?", Long.class, "TEST-P8-" + suffix);
    }

    private JsonNode calculate(String token, long taskId) throws Exception {
        String response = mockMvc.perform(post("/saving/tasks/" + taskId + "/calculate")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"observationDays\":7,\"basePrice\":0.65}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data");
    }

    @Test
    @DisplayName("按修正基线核算并支持复算与版本追溯")
    void calculateAndRecompute() throws Exception {
        String token = login("admin", "Admin@123");
        long taskId = createTask("复算");

        JsonNode first = calculate(token, taskId);
        assertThat(first.path("conclusionCode").asInt()).isEqualTo(1);
        assertThat(first.path("savingValue").asDouble()).isCloseTo(336.0, org.assertj.core.data.Offset.offset(0.5));
        assertThat(first.path("savingRate").asDouble()).isCloseTo(20.0, org.assertj.core.data.Offset.offset(0.05));
        assertThat(first.path("completeness").asDouble()).isEqualTo(100.0);
        assertThat(first.path("version").asInt()).isEqualTo(1);
        assertThat(first.path("savingAmount").asDouble()).isGreaterThan(0);

        JsonNode second = calculate(token, taskId);
        assertThat(second.path("version").asInt()).isEqualTo(2);
        assertThat(second.path("versions").size()).isEqualTo(2);
        assertThat(second.path("savingValue").asDouble()).isEqualTo(first.path("savingValue").asDouble());

        long resultId = second.path("id").asLong();
        mockMvc.perform(post("/saving/results/" + resultId + "/confirm")
                        .header("Authorization", bearer(token))
                        .param("pass", "true")
                        .param("remark", "观察期数据完整，认可核算结果"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/saving/results/" + resultId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.statusLabel").value("已生效"));

        mockMvc.perform(get("/rectification/tasks/" + taskId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.status").value(4));

        mockMvc.perform(get("/saving/summary").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalSaving").isNumber())
                .andExpect(jsonPath("$.data.ranking.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("观察期数据不足标记待验证，不判节能成功")
    void insufficientData() throws Exception {
        String token = login("admin", "Admin@123");
        jdbc.update("DELETE FROM energy_baseline_detail WHERE baseline_id = ? AND period_start >= ?",
                baselineId, START.plusDays(2));
        jdbc.update("DELETE FROM production_record WHERE device_id = ? AND start_time >= ?",
                DEVICE_ID, START.plusDays(2));

        long taskId = createTask("数据不足");
        JsonNode result = calculate(token, taskId);

        assertThat(result.path("conclusionCode").asInt()).isEqualTo(4);
        assertThat(result.path("conclusion").asText()).contains("数据不足");
        assertThat(result.path("completeness").asDouble()).isLessThan(80);

        long resultId = result.path("id").asLong();
        mockMvc.perform(post("/saving/results/" + resultId + "/confirm")
                        .header("Authorization", bearer(token))
                        .param("pass", "false")
                        .param("remark", "观察期不足 7 天"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/rectification/tasks/" + taskId).header("Authorization", bearer(token)))
                .andExpect(jsonPath("$.data.status").value(2));
    }

    @Test
    @DisplayName("导出 CSV 含表头与核算明细")
    void exportCsv() throws Exception {
        String token = login("admin", "Admin@123");
        long taskId = createTask("导出");
        calculate(token, taskId);

        mockMvc.perform(get("/saving/export").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String content = result.getResponse().getContentAsString(StandardCharsets.UTF_8);
                    assertThat(content).contains("任务号");
                    assertThat(content).contains("节能量");
                    assertThat(content).contains("TEST-P8-导出");
                });
    }
}
