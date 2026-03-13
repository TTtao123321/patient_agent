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

@Component
public class RequestTraceFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestTraceFilter.class);

    public static final String TRACE_ID_KEY = "traceId";
    public static final String REQUEST_ID_HEADER = "X-Request-Id";

    @Value("${app.monitoring.slow-request-ms:1200}")
    private long slowRequestThresholdMs;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String traceId = request.getHeader(REQUEST_ID_HEADER);
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString();
        }

        MDC.put(TRACE_ID_KEY, traceId);
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
            MDC.remove(TRACE_ID_KEY);
        }
    }
}
