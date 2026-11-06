package com.smarten;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * 上下文加载测试：验证配置、安全链、MyBatis-Plus 插件可正常装配。
 *
 * <p>不连接数据库；如后续引入数据库初始化，请改用 Testcontainers 或独立测试库。</p>
 */
@SpringBootTest
@ActiveProfiles("test")
class SmartenApplicationTests {

    @Test
    void contextLoads() {
        // 上下文启动成功即通过
    }
}
