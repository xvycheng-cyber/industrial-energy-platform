package com.smarten.module.masterdata;

import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 企业数据隔离与接口权限测试。
 */
class DataIsolationTest extends IntegrationTestSupport {

    @Test
    @DisplayName("普通用户只能看到自己所属企业")
    void engineerSeesOwnEnterpriseOnly() throws Exception {
        String token = login("engineer", "Engineer@123");
        mockMvc.perform(get("/master/enterprises")
                        .header("Authorization", bearer(token))
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(1))
                .andExpect(jsonPath("$.data.records", hasSize(1)))
                .andExpect(jsonPath("$.data.records[0].id").value(1));
    }

    @Test
    @DisplayName("平台管理员可看到全部企业")
    void adminSeesAllEnterprises() throws Exception {
        String token = login("admin", "Admin@123");
        mockMvc.perform(get("/master/enterprises")
                        .header("Authorization", bearer(token))
                        .param("pageNum", "1")
                        .param("pageSize", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(org.hamcrest.Matchers.greaterThanOrEqualTo(2)));
    }

    @Test
    @DisplayName("越权访问其他企业详情返回 403")
    void crossEnterpriseAccessDenied() throws Exception {
        String token = login("engineer", "Engineer@123");
        mockMvc.perform(get("/master/enterprises/2").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("无权限接口返回 403（工程师无用户管理权限）")
    void noPermissionOnUserList() throws Exception {
        String token = login("engineer", "Engineer@123");
        mockMvc.perform(get("/system/users").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
    }

    @Test
    @DisplayName("工程师不能新增企业")
    void engineerCannotCreateEnterprise() throws Exception {
        String token = login("engineer", "Engineer@123");
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/master/enterprises")
                        .header("Authorization", bearer(token))
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"ENT-X\",\"name\":\"越权企业\"}"))
                .andExpect(status().isForbidden());
    }
}
