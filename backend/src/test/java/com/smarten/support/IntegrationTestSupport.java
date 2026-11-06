package com.smarten.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 集成测试基类：连接本机 MySQL 的 smarten_energy_test 库（启动时自动建表 + 种子数据）。
 *
 * <p>运行前需保证 MySQL 可连接；测试数据使用随机编码，避免重复执行冲突。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestSupport {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    protected String login(String username, String password) throws Exception {
        String body = objectMapper.writeValueAsString(
                java.util.Map.of("username", username, "password", password));
        String response = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        JsonNode node = objectMapper.readTree(response);
        if (node.path("code").asInt() != 200) {
            throw new IllegalStateException("登录失败: " + response);
        }
        return node.path("data").path("accessToken").asText();
    }

    protected String bearer(String token) {
        return "Bearer " + token;
    }

    protected String uniqueCode(String prefix) {
        return prefix + System.currentTimeMillis() % 1_000_000;
    }
}
