package com.smarten;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 智能耗 · 工业企业能耗画像与节能诊断平台 启动类。
 *
 * <p>Mapper 接口使用 {@code @Mapper} 注解，由 MyBatis-Plus 自动扫描注册；
 * 模块开发时请勿使用通配符形式的 {@code @MapperScan}。</p>
 */
@SpringBootApplication
@ConfigurationPropertiesScan
@EnableScheduling
@EnableAsync
public class SmartenApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartenApplication.class, args);
    }
}
