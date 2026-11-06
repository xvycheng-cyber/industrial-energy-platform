package com.smarten.module.rectification;

import com.fasterxml.jackson.databind.JsonNode;
import com.smarten.support.IntegrationTestSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 整改闭环：创建→分配→小程序登录→开始→反馈→审核→效果验证；驳回、越权、上传校验。
 */
class RectificationFlowTest extends IntegrationTestSupport {

    @Autowired
    private JdbcTemplate jdbc;

    private Long engineerId() {
        return jdbc.queryForObject("SELECT id FROM sys_user WHERE username = 'engineer'", Long.class);
    }

    private JsonNode createTask(String token, String title) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("deviceId", 1);
        body.put("title", title);
        body.put("content", "电机空载损耗偏高");
        body.put("measure", "优化启停策略并检查传动");
        body.put("expectedSaving", 120);
        body.put("priority", 2);
        body.put("assigneeId", engineerId());
        body.put("deadline", "2030-01-01 00:00:00");
        String response = mockMvc.perform(post("/rectification/tasks")
                        .header("Authorization", bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return objectMapper.readTree(response).path("data");
    }

    private String mobileToken() throws Exception {
        return login("engineer", "Engineer@123");
    }

    @AfterEach
    void cleanup() {
        jdbc.update("DELETE f FROM rectification_feedback f JOIN rectification_task t ON t.id = f.task_id WHERE t.title LIKE 'TEST-P7%'");
        jdbc.update("DELETE FROM rectification_task WHERE title LIKE 'TEST-P7%'");
    }

    @Test
    @DisplayName("完整闭环：分配→开始→反馈→审核→效果验证")
    void fullLoop() throws Exception {
        String admin = login("admin", "Admin@123");
        JsonNode task = createTask(admin, "TEST-P7-闭环");
        long id = task.path("id").asLong();
        assertThat(task.path("status").asInt()).isEqualTo(1);

        String mobile = mobileToken();
        mockMvc.perform(get("/mobile/tasks/" + id).header("Authorization", bearer(mobile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.assigneeId").value(engineerId()));

        mockMvc.perform(post("/mobile/tasks/" + id + "/start").header("Authorization", bearer(mobile)))
                .andExpect(status().isOk());

        String feedback = """
                {"content":"已更换轴承并优化启停","beforeDesc":"空载电流偏高","afterDesc":"空载电流恢复正常",
                 "actualSaving":95.5,"complete":true,"images":["/uploads/demo/a.png"]}
                """;
        mockMvc.perform(post("/mobile/tasks/" + id + "/feedback")
                        .header("Authorization", bearer(mobile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(feedback))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/rectification/tasks/" + id).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.data.status").value(3))
                .andExpect(jsonPath("$.data.feedbacks.length()", org.hamcrest.Matchers.greaterThanOrEqualTo(2)));

        mockMvc.perform(post("/rectification/tasks/" + id + "/audit")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pass\":true,\"remark\":\"资料齐全\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/rectification/tasks/" + id).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.data.status").value(7))
                .andExpect(jsonPath("$.data.statusLabel").value("效果验证中"));

        mockMvc.perform(post("/rectification/tasks/" + id + "/verify")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pass\":true,\"actualSaving\":90,\"remark\":\"观察期能耗下降\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/rectification/tasks/" + id).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.data.status").value(4))
                .andExpect(jsonPath("$.data.progress").value(100))
                .andExpect(jsonPath("$.data.statusLabel").value("已完成"));

        // 已完成后不允许再次验证
        mockMvc.perform(post("/rectification/tasks/" + id + "/verify")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pass\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(6001));
    }

    @Test
    @DisplayName("审核驳回必须填写原因，驳回后可转派重新整改")
    void rejectRequiresReasonAndTransfer() throws Exception {
        String admin = login("admin", "Admin@123");
        long id = createTask(admin, "TEST-P7-驳回").path("id").asLong();
        String mobile = mobileToken();

        mockMvc.perform(post("/mobile/tasks/" + id + "/start").header("Authorization", bearer(mobile)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/mobile/tasks/" + id + "/feedback")
                        .header("Authorization", bearer(mobile))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"已处理\",\"complete\":true}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/rectification/tasks/" + id + "/audit")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pass\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));

        mockMvc.perform(post("/rectification/tasks/" + id + "/audit")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"pass\":false,\"reason\":\"现场照片不清晰，请补充\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        mockMvc.perform(get("/rectification/tasks/" + id).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.data.status").value(5))
                .andExpect(jsonPath("$.data.rejectReason").value("现场照片不清晰，请补充"));

        mockMvc.perform(post("/rectification/tasks/" + id + "/transfer")
                        .header("Authorization", bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"assigneeId\":" + engineerId() + ",\"reason\":\"重新整改\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/rectification/tasks/" + id).header("Authorization", bearer(admin)))
                .andExpect(jsonPath("$.data.status").value(1))
                .andExpect(jsonPath("$.data.transferCount").value(1));
    }

    @Test
    @DisplayName("工程师不能查看/操作非本人任务")
    void ownershipEnforced() throws Exception {
        String admin = login("admin", "Admin@123");
        jdbc.update("""
                INSERT INTO rectification_task (task_no, enterprise_id, device_id, title, status, assignee_id,
                                                assignee_name, priority, expected_saving, progress, source, deleted)
                VALUES ('TEST-P7-OTHER', 1, 1, 'TEST-P7-他人任务', 1, 1, '平台管理员', 2, 10, 0, 2, 0)
                """);
        Long otherId = jdbc.queryForObject("SELECT id FROM rectification_task WHERE task_no = 'TEST-P7-OTHER'", Long.class);
        String mobile = mobileToken();

        mockMvc.perform(get("/mobile/tasks/" + otherId).header("Authorization", bearer(mobile)))
                .andExpect(status().isForbidden());
        mockMvc.perform(post("/mobile/tasks/" + otherId + "/start").header("Authorization", bearer(mobile)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("小程序登录校验工程师身份")
    void mobileLogin() throws Exception {
        String response = mockMvc.perform(post("/mobile/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"engineer\",\"password\":\"Engineer@123\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(objectMapper.readTree(response).path("code").asInt()).isEqualTo(200);
        assertThat(objectMapper.readTree(response).path("data").path("accessToken").asText()).isNotBlank();

        jdbc.update("DELETE FROM sys_user_role WHERE user_id IN (SELECT id FROM sys_user WHERE username = 'viewer_t7')");
        jdbc.update("DELETE FROM sys_user WHERE username = 'viewer_t7'");
        jdbc.update("INSERT INTO sys_user (username, password, real_name, enterprise_id, status, deleted) "
                + "SELECT 'viewer_t7', password, '只读用户', 1, 1, 0 FROM sys_user WHERE username = 'engineer'");
        jdbc.update("INSERT INTO sys_user_role (user_id, role_id) "
                + "SELECT u.id, r.id FROM sys_user u, sys_role r "
                + "WHERE u.username = 'viewer_t7' AND r.role_code = 'VIEWER'");
        mockMvc.perform(post("/mobile/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"viewer_t7\",\"password\":\"Engineer@123\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403));
        jdbc.update("DELETE FROM sys_user_role WHERE user_id = (SELECT id FROM sys_user WHERE username = 'viewer_t7')");
        jdbc.update("DELETE FROM sys_user WHERE username = 'viewer_t7'");
    }

    @Test
    @DisplayName("现场图片上传：仅图片且限制大小")
    void uploadValidation() throws Exception {
        String mobile = mobileToken();
        MockMultipartFile png = new MockMultipartFile("file", "scene.png", "image/png", new byte[]{1, 2, 3, 4});
        mockMvc.perform(multipart("/mobile/upload").file(png).header("Authorization", bearer(mobile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.url").isNotEmpty());

        MockMultipartFile txt = new MockMultipartFile("file", "note.txt", "text/plain", "hello".getBytes());
        mockMvc.perform(multipart("/mobile/upload").file(txt).header("Authorization", bearer(mobile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    @DisplayName("小程序异常列表与个人中心")
    void mobileProfileAndAnomalies() throws Exception {
        String mobile = mobileToken();
        mockMvc.perform(get("/mobile/profile").header("Authorization", bearer(mobile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.username").value("engineer"))
                .andExpect(jsonPath("$.data.roles[0]").value("ENGINEER"));

        mockMvc.perform(get("/mobile/anomalies").header("Authorization", bearer(mobile)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }
}
