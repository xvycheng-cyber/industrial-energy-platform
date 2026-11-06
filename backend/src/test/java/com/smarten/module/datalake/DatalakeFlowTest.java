package com.smarten.module.datalake;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 数据湖全链路：ODS 接入 → DWD 清洗 → DWS 聚合 → ADS/完整率，并验证幂等与 99% 完整率。
 */
class DatalakeFlowTest extends IntegrationTestSupport {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("一天 1440 点采集：完整率 100%、清洗与聚合正确、重复提交幂等")
    void fullDayPipeline() throws Exception {
        String token = login("admin", "Admin@123");
        LocalDate date = LocalDate.of(2031, 1, 1)
                .plusDays(ThreadLocalRandom.current().nextInt(3000));
        // 清理同一天的历史测试数据，保证用例可重复执行
        jdbc.update("DELETE FROM energy_raw_data WHERE collect_time >= ? AND collect_time < ?",
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        jdbc.update("DELETE FROM energy_detail WHERE period_start >= ? AND period_start < ?",
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        jdbc.update("DELETE FROM energy_aggregate WHERE period_start >= ? AND period_start < ?",
                date.atStartOfDay(), date.plusDays(1).atStartOfDay());
        jdbc.update("DELETE FROM data_ingestion_log WHERE batch_no LIKE ?", "TEST-" + date + "-%");
        jdbc.update("DELETE FROM data_ingest_failure WHERE batch_no LIKE ?", "TEST-" + date + "-%");

        String batchNo = "TEST-" + date + "-" + System.currentTimeMillis();

        String payload = buildBatch(batchNo, date, 1440);

        // 1. ODS 接入
        String ingestResponse = mockMvc.perform(post("/datalake/ingest")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.insertedCount").value(1440))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode ingest = objectMapper.readTree(ingestResponse);
        assertThat(ingest.path("data").path("batchNo").asText()).isEqualTo(batchNo);

        // 2. 原始数据已入库
        mockMvc.perform(get("/datalake/raw")
                        .header("Authorization", bearer(token))
                        .param("batchNo", batchNo)
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1440));

        // 3. 幂等：同批次重复提交不重复入库
        mockMvc.perform(post("/datalake/ingest")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.insertedCount").value(1440));

        mockMvc.perform(get("/datalake/raw")
                        .header("Authorization", bearer(token))
                        .param("batchNo", batchNo)
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(jsonPath("$.data.total").value(1440));

        // 4. DWD 清洗：首个点为基线，其余 1439 个点生成明细
        mockMvc.perform(post("/datalake/clean")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchNo\":\"" + batchNo + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/datalake/details")
                        .header("Authorization", bearer(token))
                        .param("meterId", "1")
                        .param("start", date + " 00:00:00")
                        .param("end", date.plusDays(1) + " 00:00:00")
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1439));

        // 5. DWS 日聚合：1439 × 0.5 = 719.5
        mockMvc.perform(post("/datalake/aggregate")
                        .header("Authorization", bearer(token))
                        .param("date", date.toString())
                        .param("periods", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String aggregateResponse = mockMvc.perform(get("/datalake/aggregates")
                        .header("Authorization", bearer(token))
                        .param("dimType", "4")
                        .param("dimId", "1")
                        .param("periodType", "2")
                        .param("start", date + " 00:00:00")
                        .param("end", date.plusDays(1) + " 00:00:00")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode aggregate = objectMapper.readTree(aggregateResponse);
        double energy = aggregate.path("data").path("records").get(0).path("energyValue").asDouble();
        assertThat(energy).isCloseTo(719.5, org.assertj.core.data.Offset.offset(0.01));

        // 6. 完整率：当天 1440 点全部有效
        mockMvc.perform(get("/datalake/completeness")
                        .header("Authorization", bearer(token))
                        .param("date", date.toString())
                        .param("deviceId", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.expectedCount").value(1440))
                .andExpect(jsonPath("$.data.validCount").value(1440))
                .andExpect(jsonPath("$.data.missingCount").value(0))
                .andExpect(jsonPath("$.data.completenessRate").value(100.00));
    }

    @Test
    @DisplayName("重复读数单独批次：计入重复，不产生重复明细")
    void duplicateReadingIsIdempotent() throws Exception {
        String token = login("admin", "Admin@123");
        LocalDate date = LocalDate.of(2032, 1, 1)
                .plusDays(ThreadLocalRandom.current().nextInt(2000));
        String batchNo = "DUP-" + date + "-" + System.currentTimeMillis();

        String payload = buildBatch(batchNo, date, 5);
        mockMvc.perform(post("/datalake/ingest")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(jsonPath("$.data.insertedCount").value(5));

        // 同一批次号再次提交：幂等，不重复
        mockMvc.perform(post("/datalake/ingest")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(jsonPath("$.data.insertedCount").value(5));

        mockMvc.perform(get("/datalake/raw")
                        .header("Authorization", bearer(token))
                        .param("batchNo", batchNo)
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(jsonPath("$.data.total").value(5));

        // 新批次提交同一时刻读数：被唯一键拦截，原始数据总量不变
        String duplicateBatch = batchNo + "-DUP";
        String duplicatePayload = buildBatch(duplicateBatch, date, 5);
        mockMvc.perform(post("/datalake/ingest")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicatePayload))
                .andExpect(jsonPath("$.data.insertedCount").value(0))
                .andExpect(jsonPath("$.data.failedCount").value(5));

        mockMvc.perform(get("/datalake/raw")
                        .header("Authorization", bearer(token))
                        .param("batchNo", batchNo)
                        .param("pageNum", "1")
                        .param("pageSize", "1"))
                .andExpect(jsonPath("$.data.total").value(5));
    }

    private String buildBatch(String batchNo, LocalDate date, int points) throws Exception {
        List<Map<String, Object>> readings = new ArrayList<>(points);
        LocalDateTime start = date.atStartOfDay();
        for (int i = 0; i < points; i++) {
            Map<String, Object> reading = new LinkedHashMap<>();
            reading.put("meterId", 1);
            reading.put("collectTime", start.plusMinutes(i).format(TIME_FORMAT));
            reading.put("value", 1000 + i * 0.5);
            reading.put("readingType", 1);
            reading.put("pointCode", "TOTAL");
            readings.add(reading);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("batchNo", batchNo);
        body.put("source", "API");
        body.put("readings", readings);
        return objectMapper.writeValueAsString(body);
    }
}
