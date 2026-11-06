package com.smarten.module.system;

import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 用户管理：新增、重复校验、禁用、重置密码、删除。
 */
class UserCrudTest extends IntegrationTestSupport {

    @Test
    @DisplayName("新增用户后可用新账号登录，删除后不可登录")
    void createLoginAndDelete() throws Exception {
        String adminToken = login("admin", "Admin@123");
        String username = "u" + System.currentTimeMillis() % 1_000_000;

        String body = objectMapper.writeValueAsString(Map.of(
                "username", username,
                "password", "Test@123456",
                "realName", "测试用户",
                "enterpriseId", 1,
                "roleIds", List.of(4),
                "status", 1));
        String response = mockMvc.perform(post("/system/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long userId = objectMapper.readTree(response).path("data").asLong();
        assertThat(userId).isPositive();

        // 新账号可登录，且只读角色拥有设备查看权限
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", "Test@123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.user.roles[0]").value("VIEWER"));

        // 禁用后登录被拒绝
        mockMvc.perform(put("/system/users/" + userId + "/status")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":0}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", "Test@123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1002));

        // 删除用户
        mockMvc.perform(delete("/system/users/" + userId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", "Test@123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("用户名重复返回 1005")
    void duplicateUsername() throws Exception {
        String adminToken = login("admin", "Admin@123");
        String body = objectMapper.writeValueAsString(Map.of(
                "username", "engineer",
                "password", "Test@123456",
                "enterpriseId", 1,
                "roleIds", List.of(4)));
        mockMvc.perform(post("/system/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1005));
    }

    @Test
    @DisplayName("重置密码后新密码生效")
    void resetPassword() throws Exception {
        String adminToken = login("admin", "Admin@123");
        String username = "p" + System.currentTimeMillis() % 1_000_000;

        String response = mockMvc.perform(post("/system/users")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username,
                                "password", "Init@123456",
                                "enterpriseId", 1,
                                "roleIds", List.of(4)))))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long userId = objectMapper.readTree(response).path("data").asLong();

        mockMvc.perform(put("/system/users/" + userId + "/password")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"New@123456\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "username", username, "password", "New@123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(delete("/system/users/" + userId).header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk());
    }
}
