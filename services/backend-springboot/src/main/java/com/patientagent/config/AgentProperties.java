package com.patientagent.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * FastAPI Agent 服务的配置属性，绑定配置文件中 {@code agent.*} 前缀的值。
 * <p>示例（application.yml）：</p>
 * <pre>
 * agent:
 *   base-url: http://127.0.0.1:8000
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    /** FastAPI Agent 服务的根 URL，不含路径后缀（末尾不需要斜杠）。 */
    private String baseUrl;

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
