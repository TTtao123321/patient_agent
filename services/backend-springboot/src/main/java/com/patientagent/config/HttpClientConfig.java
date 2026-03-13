package com.patientagent.config;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * HTTP 客户端配置类，向 Spring 容器注册 {@link RestTemplate} Bean。
 * <p>
 * 连接超时 5 秒，读取超时 30 秒，用于同步调用 FastAPI Agent 服务。
 * 流式（SSE）请求使用单独的 JDK {@code HttpClient}，在 {@code AgentHttpClient} 中自行创建。
 * </p>
 */
@Configuration
public class HttpClientConfig {

    /**
     * 创建并配置 {@link RestTemplate}，设置连接和读取超时。
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))   // 建立 TCP 连接最长等待时间
                .setReadTimeout(Duration.ofSeconds(30))     // 等待响应数据的最长时间
                .build();
    }
}
