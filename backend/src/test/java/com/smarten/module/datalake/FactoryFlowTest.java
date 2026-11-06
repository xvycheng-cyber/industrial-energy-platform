package com.smarten.module.datalake;

import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 三维工厂：场景布局、风险等级、设备/产线详情。
 */
class FactoryFlowTest extends IntegrationTestSupport {

    @Test
    @DisplayName("场景返回车间/产线/设备布局坐标与风险")
    void scene() throws Exception {
        String token = login("admin", "Admin@123");
        mockMvc.perform(get("/factory/scene").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.enterpriseName").isNotEmpty())
                .andExpect(jsonPath("$.data.workshops.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.workshops[0].position.x").isNumber())
                .andExpect(jsonPath("$.data.workshops[0].lines[0].devices[0].position.x").isNumber())
                .andExpect(jsonPath("$.data.workshops[0].lines[0].devices[0].risk").isNotEmpty())
                .andExpect(jsonPath("$.data.deviceCount", org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("设备详情含风险与能耗，基线与诊断标记不可用")
    void deviceDetail() throws Exception {
        String token = login("admin", "Admin@123");
        mockMvc.perform(get("/factory/devices/1").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.code").value("DEV001"))
                .andExpect(jsonPath("$.data.statusLabel").isNotEmpty())
                .andExpect(jsonPath("$.data.risk").isNotEmpty())
                .andExpect(jsonPath("$.data.energyToday").isNumber())
                .andExpect(jsonPath("$.data.baselineAvailable").value(false))
                .andExpect(jsonPath("$.data.anomalyAvailable").value(false));
    }

    @Test
    @DisplayName("产线详情返回风险分布与设备统计")
    void lineDetail() throws Exception {
        String token = login("admin", "Admin@123");
        mockMvc.perform(get("/factory/lines/1").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").isNotEmpty())
                .andExpect(jsonPath("$.data.deviceCount").isNumber())
                .andExpect(jsonPath("$.data.riskCounts.NORMAL").isNumber())
                .andExpect(jsonPath("$.data.baselineAvailable").value(false));
    }

    @Test
    @DisplayName("工程师请求其他企业时被强制回本企业")
    void isolation() throws Exception {
        String token = login("engineer", "Engineer@123");
        mockMvc.perform(get("/factory/scene").param("enterpriseId", "2").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.enterpriseId").value(1));
    }
}
