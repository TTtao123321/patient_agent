package com.patientagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 患者 AI 助手系统 - Spring Boot 主启动类。
 * <p>
 * {@code @SpringBootApplication} 会自动完成组件扫描、自动配置与 Bean 注册，
 * 启动后 Spring 上下文加载所有 Controller / Service / Repository 等组件。
 * </p>
 */
@SpringBootApplication
public class PatientAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(PatientAgentApplication.class, args);
    }
}
