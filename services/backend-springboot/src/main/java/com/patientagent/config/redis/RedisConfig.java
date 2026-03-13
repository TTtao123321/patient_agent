package com.patientagent.config.redis;

import org.springframework.context.annotation.Configuration;
import org.springframework.session.data.redis.config.annotation.web.http.EnableRedisHttpSession;

/**
 * Redis 配置类。
 * <p>
 * 通过 {@code @EnableRedisHttpSession} 将 Spring Session 的存储后端切换为 Redis，
 * 使 HTTP Session 在多实例部署场景下也能共享。
 * 实际 Redis 连接参数由 {@code spring.data.redis.*} 配置项提供。
 * </p>
 */
@Configuration
@EnableRedisHttpSession
public class RedisConfig {
}
