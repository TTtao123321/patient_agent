package com.patientagent.client.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.patientagent.client.agent.dto.AgentChatRequest;
import com.patientagent.client.agent.dto.AgentStreamEvent;
import com.patientagent.common.tracing.RequestTraceFilter;
import com.patientagent.config.AgentProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.function.Consumer;

/**
 * FastAPI Agent 服务的 HTTP 客户端适配器，实现 {@link AgentClient} 接口。
 * <p>
 * 提供两种调用模式：
 * <ul>
 *   <li>{@link #chat} — 使用 Spring {@code RestTemplate} 发起同步 POST 请求。</li>
 *   <li>{@link #streamChat} — 使用 JDK11+ {@code HttpClient} 接收 SSE 流，逐帧解析后回调。</li>
 * </ul>
 * 调用均会将当前请求的 MDC traceId 通过 {@code X-Request-Id} 头传递给 FastAPI，
 * 实现端到端链路追踪。
 * </p>
 */
@Component
public class AgentHttpClient implements AgentClient {

    private static final Logger log = LoggerFactory.getLogger(AgentHttpClient.class);

    /** 慢 Agent 调用告警阈值（毫秒）。 */
    @Value("${app.monitoring.slow-agent-call-ms:1500}")
    private long slowAgentCallThresholdMs;

    private final RestTemplate restTemplate;
    private final AgentProperties agentProperties;
    private final ObjectMapper objectMapper;
    /** JDK 原生 HTTP 客户端，用于 SSE 流式请求（Spring RestTemplate 不支持流式语义）。 */
    private final HttpClient httpClient;

    /** 构造时初始化 JDK HttpClient，连接超时 5 秒。 */
    public AgentHttpClient(RestTemplate restTemplate, AgentProperties agentProperties, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.agentProperties = agentProperties;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    @Override
    public String chat(String sessionNo, Long userId, String message) {
        // 构造 FastAPI Agent 的完整 URL 路径，确保 baseUrl 格式正确
        String url = normalizeBaseUrl(agentProperties.getBaseUrl()) + "/agent/chat";
        
        // 记录调用开始时间，用于后续计算延迟和监控慢调用
        long startedAt = System.currentTimeMillis();

        // 构造发送给 FastAPI 的请求对象，封装会话、用户和消息信息
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionNo(sessionNo);  // 会话编号，用于关联对话上下文
        request.setUserId(userId);        // 用户ID，用于权限验证和用户画像
        request.setQuery(message);        // 用户的实际提问内容

        // 构造 HTTP 请求头
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);  // 设置请求体为 JSON 格式
        
        // 将当前请求的 traceId 通过 X-Request-Id 头传递到 FastAPI，实现跨服务日志关联
        // traceId 从 MDC（Mapped Diagnostic Context）中获取，由 RequestTraceFilter 前置设置
        String traceId = MDC.get(RequestTraceFilter.TRACE_ID_KEY);
        if (traceId != null && !traceId.isBlank()) {
            headers.set(RequestTraceFilter.REQUEST_ID_HEADER, traceId);
        }

        try {
            // 记录调用开始日志，便于追踪和问题排查
            log.info(
                    "agent_chat_call_started sessionNo={} userId={} messageLen={}",
                    sessionNo,
                    userId,
                    message == null ? 0 : message.length()
            );
            
            // 使用 Spring RestTemplate 发送 POST 请求到 FastAPI
            // exchange 方法可以灵活指定 HTTP 方法、请求体、响应类型
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),  // 封装请求体和请求头
                    JsonNode.class                       // 响应类型为通用 JSON 节点，便于灵活解析
            );

            // 验证响应状态码是否为 2xx 成功状态，且响应体不为空
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new IllegalStateException("Agent service returned invalid response");
            }

            // 计算本次调用的总耗时（毫秒）
            long latencyMs = System.currentTimeMillis() - startedAt;
            
            // 记录调用完成日志，包含状态码和耗时
            log.info(
                    "agent_chat_call_finished sessionNo={} userId={} status={} latencyMs={}",
                    sessionNo,
                    userId,
                    response.getStatusCode().value(),
                    latencyMs
            );
            
            // 检查是否超过慢调用阈值，超过则输出警告日志
            // 慢调用阈值可通过配置 app.monitoring.slow-agent-call-ms 调整，默认 1500ms
            if (latencyMs >= slowAgentCallThresholdMs) {
                log.warn(
                        "slow_agent_chat_call_detected sessionNo={} userId={} latencyMs={} thresholdMs={}",
                        sessionNo,
                        userId,
                        latencyMs,
                        slowAgentCallThresholdMs
                );
            }

            // 从 JSON 响应中解析出 answer 字段并返回
            // parseAnswer 方法兼容两种响应格式：{answer:"..."} 或 {data:{answer:"..."}}
            return parseAnswer(response.getBody());
            
        } catch (RestClientException ex) {
            // 捕获 RestTemplate 抛出的 HTTP 层异常（如连接超时、4xx/5xx 错误等）
            // 将底层 HTTP 错误包装为业务可读的异常，便于上层统一处理
            log.error("agent_chat_call_failed sessionNo={} userId={} error={}", sessionNo, userId, ex.getMessage());
            throw new IllegalStateException("Failed to call agent service: " + ex.getMessage(), ex);
        }
    }

    @Override
    public void streamChat(String sessionNo, Long userId, String message, Consumer<AgentStreamEvent> eventConsumer) {
        // 构造 FastAPI Agent 流式接口的完整 URL 路径
        String url = normalizeBaseUrl(agentProperties.getBaseUrl()) + "/agent/chat/stream";
        
        // 记录调用开始时间，用于后续计算延迟和监控慢调用
        long startedAt = System.currentTimeMillis();

        // 构造发送给 FastAPI 的请求对象，与同步 chat 方法使用相同的请求结构
        AgentChatRequest request = new AgentChatRequest();
        request.setSessionNo(sessionNo);  // 会话编号，用于关联对话上下文
        request.setUserId(userId);        // 用户ID，用于权限验证和用户画像
        request.setQuery(message);        // 用户的实际提问内容

        try {
            // 使用 Jackson ObjectMapper 将请求对象序列化为 JSON 字符串
            // JDK HttpClient 需要手动序列化请求体，不支持 RestTemplate 那样的自动转换
            String requestBody = objectMapper.writeValueAsString(request);
            
            // SSE 请求同样需要传递 traceId 头以关联流式调用的日志
            // 从 MDC 中获取 traceId，实现端到端链路追踪
            String traceId = MDC.get(RequestTraceFilter.TRACE_ID_KEY);
            
            // 使用 JDK 11+ HttpClient 的 Builder 模式构造请求
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(url))
                    .header("Content-Type", "application/json")  // 设置请求体为 JSON 格式
                    .header("Accept", "text/event-stream")        // 声明接受 SSE 流式响应
                    .timeout(Duration.ofMinutes(5));              // 设置请求超时时间为 5 分钟（流式响应可能较长）
            
            // 如果 traceId 存在，则添加到请求头中传递给 FastAPI
            if (traceId != null && !traceId.isBlank()) {
                builder.header(RequestTraceFilter.REQUEST_ID_HEADER, traceId);
            }

            // 构建最终的 POST 请求，设置请求体为 JSON 字符串
            HttpRequest httpRequest = builder
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

            // 记录流式调用开始日志
            log.info(
                    "agent_stream_call_started sessionNo={} userId={} messageLen={}",
                    sessionNo,
                    userId,
                    message == null ? 0 : message.length()
            );

            // 发送请求并获取响应，使用 InputStream 作为响应体处理器
            // 这样可以逐块读取 SSE 流，而不是等待整个响应完成
            HttpResponse<java.io.InputStream> response = httpClient.send(
                    httpRequest,
                    HttpResponse.BodyHandlers.ofInputStream()
            );

            // 验证响应状态码是否为 2xx 成功状态
            // 注意：不检查 response.body() 为 null，因为 InputStream 在响应开始时就可用
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Agent stream service returned invalid status: " + response.statusCode());
            }

            // 逐帧解析 SSE 流并通过回调函数 eventConsumer 转发给上层服务
            // parseSseStream 会持续读取 InputStream，直到流结束或发生异常
            parseSseStream(response, eventConsumer);
            
            // 计算本次流式调用的总耗时（毫秒）
            long latencyMs = System.currentTimeMillis() - startedAt;
            
            // 记录流式调用完成日志，包含状态码和总耗时
            log.info(
                    "agent_stream_call_finished sessionNo={} userId={} status={} latencyMs={}",
                    sessionNo,
                    userId,
                    response.statusCode(),
                    latencyMs
            );
            
            // 检查是否超过慢调用阈值，超过则输出警告日志
            // 注意：流式调用的耗时通常比同步调用长，因为需要等待完整响应生成
            if (latencyMs >= slowAgentCallThresholdMs) {
                log.warn(
                        "slow_agent_stream_call_detected sessionNo={} userId={} latencyMs={} thresholdMs={}",
                        sessionNo,
                        userId,
                        latencyMs,
                        slowAgentCallThresholdMs
                );
            }
            
        } catch (IOException ex) {
            // 捕获 IO 异常，包括：
            // - 连接失败
            // - 读取流时网络中断
            // - JSON 序列化失败
            log.error("agent_stream_call_failed sessionNo={} userId={} error={}", sessionNo, userId, ex.getMessage());
            throw new IllegalStateException("Failed to read agent stream: " + ex.getMessage(), ex);
            
        } catch (InterruptedException ex) {
            // 捕获线程中断异常，当调用线程被 interrupt() 时触发
            // 恢复中断状态（良好的并发编程实践），然后抛出业务异常
            Thread.currentThread().interrupt();
            log.error("agent_stream_call_interrupted sessionNo={} userId={}", sessionNo, userId);
            throw new IllegalStateException("Agent stream request interrupted", ex);
        }
    }

    /**
     * 解析 Agent 返回的 JSON 中的 answer 字段。
     * FastAPI 当前返回的回答字段可能存在根层（{@code answer}）
     * 或内嵌在 data 对象下（{@code data.answer}）。
     */
    private String parseAnswer(JsonNode body) {
        if (body.hasNonNull("answer")) {
            return body.get("answer").asText();
        }
        if (body.has("data") && body.get("data").hasNonNull("answer")) {
            return body.get("data").get("answer").asText();
        }
        throw new IllegalStateException("Agent response does not contain answer field");
    }

    /**
     * 按行解析 SSE 响应流，实现标准的 Server-Sent Events 协议解析。
     * <p>
     * SSE 协议规则：
     * <ul>
     *   <li>每一行以字段名开头，如 "event:"、"data:"</li>
     *   <li>空行表示一个事件帧的结束</li>
     *   <li>同一事件可以有多个 "data:" 行，用换行符连接</li>
     *   <li>如果没有 "event:" 行，默认事件名为 "message"</li>
     * </ul>
     * </p>
     * @param response HTTP 响应对象，包含 InputStream
     * @param eventConsumer 事件消费回调，每解析完一个事件帧就调用一次
     */
    private void parseSseStream(
            HttpResponse<java.io.InputStream> response,
            Consumer<AgentStreamEvent> eventConsumer
    ) throws IOException {
        // 使用 BufferedReader 按行读取 SSE 流
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.body(), StandardCharsets.UTF_8)
        )) {
            String currentEvent = "message";  // 默认事件名
            StringBuilder dataBuffer = new StringBuilder();  // 累积当前事件的数据
            String line;

            // 逐行读取直到流结束
            while ((line = reader.readLine()) != null) {
                // 空行表示一个事件帧结束，触发事件分发
                if (line.isEmpty()) {
                    dispatchSseEvent(currentEvent, dataBuffer, eventConsumer);
                    currentEvent = "message";  // 重置为默认事件名
                    dataBuffer.setLength(0);   // 清空数据缓冲区
                    continue;
                }

                // 解析 "event:" 行：设置当前事件名
                if (line.startsWith("event:")) {
                    currentEvent = line.substring("event:".length()).trim();
                    continue;
                }

                // 解析 "data:" 行：追加到数据缓冲区
                if (line.startsWith("data:")) {
                    // 如果已有数据，先添加换行符（支持多行 data）
                    if (dataBuffer.length() > 0) {
                        dataBuffer.append('\n');
                    }
                    dataBuffer.append(line.substring("data:".length()).trim());
                }
            }

            // 流结束后，处理最后一个可能还未分发的事件
            dispatchSseEvent(currentEvent, dataBuffer, eventConsumer);
        }
    }

    /**
     * 将一帧 SSE 数据解析为 {@link AgentStreamEvent} 对象并通过回调转发给上层。
     * <p>
     * 该方法会：
     * <ol>
     *   <li>检查数据缓冲区是否为空（空的话跳过，如心跳帧）</li>
     *   <li>将 JSON 字符串解析为 JsonNode</li>
     *   <li>创建 AgentStreamEvent 对象</li>
     *   <li>调用 eventConsumer 回调函数</li>
     * </ol>
     * </p>
     * @param eventName SSE 事件名，如 "start"、"chunk"、"done"
     * @param dataBuffer 包含 JSON 数据的字符串缓冲区
     * @param eventConsumer 事件消费回调函数
     */
    private void dispatchSseEvent(
            String eventName,
            StringBuilder dataBuffer,
            Consumer<AgentStreamEvent> eventConsumer
    ) throws IOException {
        // 如果数据缓冲区为空（如心跳帧或注释行），则跳过
        if (dataBuffer.length() == 0) {
            return;
        }

        // 将 JSON 字符串解析为 Jackson JsonNode
        JsonNode dataNode = objectMapper.readTree(dataBuffer.toString());
        
        // 创建 AgentStreamEvent 对象并通过回调转发
        eventConsumer.accept(new AgentStreamEvent(eventName, dataNode));
    }

    /**
     * 规范化 baseUrl 格式，确保：
     * <ol>
     *   <li>baseUrl 不为 null 或空字符串</li>
     *   <li>不以斜杠结尾，避免与后续路径拼接时出现双斜杠</li>
     * </ol>
     * <p>
     * 例如：
     * <ul>
     *   <li>输入 "http://localhost:8000/" → 输出 "http://localhost:8000"</li>
     *   <li>输入 "http://localhost:8000" → 输出 "http://localhost:8000"</li>
     * </ul>
     * </p>
     * @param baseUrl 原始的 baseUrl 配置值
     * @return 规范化后的 baseUrl
     * @throws IllegalStateException 如果 baseUrl 为 null 或空
     */
    private String normalizeBaseUrl(String baseUrl) {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalStateException("agent.base-url is not configured");
        }
        // 如果以斜杠结尾，移除最后一个斜杠
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1);
        }
        return baseUrl;
    }
}
