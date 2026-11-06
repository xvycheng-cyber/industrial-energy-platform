package com.smarten.module.datalake;

import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 驾驶舱与画像：指标结构、趋势、层级下钻、画像接口。
 */
class ProfileFlowTest extends IntegrationTestSupport {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Test
    @DisplayName("有数据时驾驶舱返回 KPI、结构与趋势")
    void cockpitWithData() throws Exception {
        String token = login("admin", "Admin@123");
        LocalDate date = LocalDate.of(2034, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(2000));
        String batchNo = "PROF-" + date + "-" + System.currentTimeMillis();

        List<Map<String, Object>> readings = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            Map<String, Object> reading = new LinkedHashMap<>();
            reading.put("meterId", 1);
            reading.put("collectTime", date.atStartOfDay().plusHours(i).format(TIME_FORMAT));
            reading.put("value", 500 + i * 2.0);
            reading.put("readingType", 1);
            readings.add(reading);
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("batchNo", batchNo);
        body.put("source", "API");
        body.put("readings", readings);

        mockMvc.perform(post("/datalake/ingest")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/datalake/clean")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"batchNo\":\"" + batchNo + "\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/datalake/aggregate")
                        .header("Authorization", bearer(token))
                        .param("date", date.toString()))
                .andExpect(status().isOk());

        mockMvc.perform(get("/profile/cockpit")
                        .header("Authorization", bearer(token))
                        .param("date", date.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.kpis.length()").value(8))
                .andExpect(jsonPath("$.data.scopeType").value(1))
                .andExpect(jsonPath("$.data.trend.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(20)))
                .andExpect(jsonPath("$.data.structure.length()").value(1))
                .andExpect(jsonPath("$.data.structure[0].energyValue").isNumber())
                .andExpect(jsonPath("$.data.kpis[0].available").value(true))
                .andExpect(jsonPath("$.data.kpis[4].available").value(false));
    }

    @Test
    @DisplayName("层级树与画像接口可用")
    void hierarchyAndProfiles() throws Exception {
        String token = login("admin", "Admin@123");

        mockMvc.perform(get("/profile/hierarchy").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].type").value("enterprise"))
                .andExpect(jsonPath("$.data[0].children[0].type").value("workshop"))
                .andExpect(jsonPath("$.data[0].children[0].children[0].type").value("line"));

        mockMvc.perform(get("/profile/enterprise/1").header("Authorization", bearer(token)).param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.name").isNotEmpty())
                .andExpect(jsonPath("$.data.rating.grade").isNotEmpty());

        mockMvc.perform(get("/profile/line/1").header("Authorization", bearer(token)).param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").isNotEmpty())
                .andExpect(jsonPath("$.data.deviceCount").isNumber());

        mockMvc.perform(get("/profile/device/1").header("Authorization", bearer(token)).param("days", "7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("DEV001"));
    }

    @Test
    @DisplayName("工程师只能看本企业画像")
    void isolation() throws Exception {
        String token = login("engineer", "Engineer@123");
        mockMvc.perform(get("/profile/enterprise/2").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }
}
