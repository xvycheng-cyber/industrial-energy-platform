package com.smarten.module.system;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 登录与鉴权集成测试。
 */
class AuthControllerTest extends IntegrationTestSupport {

    @Test
    @DisplayName("管理员登录成功并返回令牌与权限")
    void loginSuccess() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"Admin@123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.username").value("admin"))
                .andExpect(jsonPath("$.data.user.roles[0]").value("PLATFORM_ADMIN"))
                .andExpect(jsonPath("$.data.user.permissions",
                        org.hamcrest.Matchers.hasSize(org.hamcrest.Matchers.greaterThanOrEqualTo(20))));
    }

    @Test
    @DisplayName("密码错误返回业务错误码 1001")
    void loginWithWrongPassword() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"wrong-password\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(1001));
    }

    @Test
    @DisplayName("未携带令牌访问受保护接口返回 401")
    void accessWithoutToken() throws Exception {
        mockMvc.perform(get("/system/users"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    @DisplayName("携带令牌可访问用户列表")
    void accessWithToken() throws Exception {
        String token = login("admin", "Admin@123");
        mockMvc.perform(get("/system/users")
                        .header("Authorization", bearer(token))
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").isNumber())
                .andExpect(jsonPath("$.data.pageNum").value(1));
    }

    @Test
    @DisplayName("当前用户接口返回角色与菜单")
    void meAndMenus() throws Exception {
        String token = login("admin", "Admin@123");
        mockMvc.perform(get("/auth/me").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("admin"));

        String response = mockMvc.perform(get("/auth/menus").header("Authorization", bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode menus = objectMapper.readTree(response).path("data");
        org.assertj.core.api.Assertions.assertThat(menus.isArray()).isTrue();
        org.assertj.core.api.Assertions.assertThat(menus.size()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("刷新令牌可换取新的访问令牌")
    void refreshToken() throws Exception {
        String token = login("admin", "Admin@123");
        JsonNode login = objectMapper.readTree(
                mockMvc.perform(get("/auth/me").header("Authorization", bearer(token)))
                        .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8));
        org.assertj.core.api.Assertions.assertThat(login.path("code").asInt()).isEqualTo(200);
    }
}
