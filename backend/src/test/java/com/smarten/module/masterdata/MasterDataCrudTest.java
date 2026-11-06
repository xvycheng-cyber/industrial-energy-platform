package com.smarten.module.masterdata;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 主数据 CRUD、分页与关联约束测试。
 */
class MasterDataCrudTest extends IntegrationTestSupport {

    @Test
    @DisplayName("车间：新增、查询、修改、删除")
    void workshopCrud() throws Exception {
        String token = login("admin", "Admin@123");
        String code = uniqueCode("WS");

        String createBody = objectMapper.writeValueAsString(Map.of(
                "enterpriseId", 1,
                "code", code,
                "name", "测试车间-" + code,
                "manager", "测试负责人",
                "area", 100.5));
        String createResponse = mockMvc.perform(post("/master/workshops")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long workshopId = objectMapper.readTree(createResponse).path("data").asLong();
        assertThat(workshopId).isPositive();

        mockMvc.perform(get("/master/workshops")
                        .header("Authorization", bearer(token))
                        .param("keyword", code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records[0].enterpriseName").value("华东精密制造有限公司"));

        mockMvc.perform(put("/master/workshops/" + workshopId)
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "code", code,
                                "name", "改名后的车间",
                                "manager", "新负责人",
                                "area", 200))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/master/workshops/" + workshopId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("改名后的车间"));

        mockMvc.perform(delete("/master/workshops/" + workshopId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("存在产线时车间不可删除（DATA_IN_USE=2006）")
    void workshopWithLineCannotBeDeleted() throws Exception {
        String token = login("admin", "Admin@123");
        String suffix = uniqueCode("");
        String code = "WSD" + suffix;

        String workshopResponse = mockMvc.perform(post("/master/workshops")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "enterpriseId", 1,
                                "code", code,
                                "name", "约束测试车间"))))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long workshopId = objectMapper.readTree(workshopResponse).path("data").asLong();

        String lineResponse = mockMvc.perform(post("/master/lines")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "workshopId", workshopId,
                                "code", "LN" + suffix,
                                "name", "约束测试产线"))))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long lineId = objectMapper.readTree(lineResponse).path("data").asLong();
        assertThat(lineId).isPositive();

        mockMvc.perform(delete("/master/workshops/" + workshopId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2006));

        mockMvc.perform(delete("/master/lines/" + lineId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        mockMvc.perform(delete("/master/workshops/" + workshopId).header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("设备分页返回关联名称与分页元数据")
    void devicePage() throws Exception {
        String token = login("admin", "Admin@123");
        mockMvc.perform(get("/master/devices")
                        .header("Authorization", bearer(token))
                        .param("pageNum", "1")
                        .param("pageSize", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.pageSize").value(2))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.records[0].lineName").isNotEmpty())
                .andExpect(jsonPath("$.data.records[0].enterpriseName").isNotEmpty());
    }

    @Test
    @DisplayName("编码重复返回 2005")
    void duplicatedCodeRejected() throws Exception {
        String token = login("admin", "Admin@123");
        String body = objectMapper.writeValueAsString(Map.of(
                "enterpriseId", 1,
                "code", "WS01",
                "name", "重复编码车间"));
        mockMvc.perform(post("/master/workshops")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(2005));
    }

    @Test
    @DisplayName("状态变更生效")
    void updateDeviceStatus() throws Exception {
        String token = login("admin", "Admin@123");
        mockMvc.perform(put("/master/devices/1/status")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        String detail = mockMvc.perform(get("/master/devices/1").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode node = objectMapper.readTree(detail);
        assertThat(node.path("data").path("status").asInt()).isEqualTo(2);

        mockMvc.perform(put("/master/devices/1/status")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":1}"))
                .andExpect(status().isOk());
    }
}
