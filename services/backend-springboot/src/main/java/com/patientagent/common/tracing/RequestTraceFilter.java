package com.patientagent.common.tracing;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * HTTP 请求链路追踪过滤器。
 * <p>
 * 每次请求进来时：
 * <ol>
 *   <li>从client/网关传入的 {@code X-Request-Id} 头或本地生成 UUID 作为 traceId。</li>
 *   <li>将 traceId 写入 SLF4J MDC，这样该线程后续所有日志条目均包含 traceId。</li>
 *   <li>将 traceId 写入响应头，便于客户端关联调用链。</li>
 *   <li>finally 块中清除 MDC，防止线程循环复用时脱漏。</li>
 * </ol>
 * </p>
 */
@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    /** MDC 键名，配置日志模板时可用 {@code %X{traceId}} 引用。 */
    public static final String TRACE_ID_KEY = "traceId";
    /** HTTP 请求/响应头名称，用于跎层间传递 trace id。 */
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    /** 慢请求告警阀値（毫秒），超过该值时输出 WARN 日志。 */
    @Value("${app.monitoring.slow-request-ms:1200}")
    private long slowRequestThresholdMs;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        // 优先使用上游网关或客户端传入的 ID，否则本地生成 UUID。
        String traceId = request.getHeader(REQUEST_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        // 将 traceId 写入 MDC，当前线程后续所有日志条目均自动携带此值。
        MDC.put(TRACE_ID_KEY, traceId);
        // 将 traceId 写回响应头，方便客户端关联同一链路的日志。
        response.setHeader(REQUEST_ID_HEADER, traceId);
        long startedAt = System.currentTimeMillis();
        log.info("http_request_started method={} uri={}", request.getMethod(), request.getRequestURI());
        try {
            filterChain.doFilter(request, response);
        } finally {
            long latencyMs = System.currentTimeMillis() - startedAt;
            log.info(
                    "http_request_finished method={} uri={} status={} latencyMs={}",
                    request.getMethod(),
                    request.getRequestURI(),
                    response.getStatus(),
                    latencyMs
            );
            if (latencyMs >= slowRequestThresholdMs) {
                log.warn(
                        "slow_http_request_detected method={} uri={} status={} latencyMs={} thresholdMs={}",
                        request.getMethod(),
                        request.getRequestURI(),
                        response.getStatus(),
                        latencyMs,
                        slowRequestThresholdMs
                );
            }
            // 在 finally 中移除 MDC，防止线程池复用时遗留旧 traceId。
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
