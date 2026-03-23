package com.patientagent.modules.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.patientagent.client.agent.AgentClient;
import com.patientagent.client.agent.dto.AgentStreamEvent;
import com.patientagent.modules.chat.dto.ChatHistoryResponse;
import com.patientagent.modules.chat.dto.ChatMessageItemResponse;
import com.patientagent.modules.chat.dto.ChatSessionItemResponse;
import com.patientagent.modules.chat.dto.ChatSessionListResponse;
import com.patientagent.modules.chat.dto.CreateSessionRequest;
import com.patientagent.modules.chat.dto.SendMessageRequest;
import com.patientagent.modules.chat.dto.SendMessageResponse;
import com.patientagent.modules.chat.entity.ChatMessageEntity;
import com.patientagent.modules.chat.entity.ChatSessionEntity;
import com.patientagent.modules.chat.repository.ChatMessageRepository;
import com.patientagent.modules.chat.repository.ChatSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 聊天应用服务实现，承担三项核心职责：
 * <ol>
 *   <li>管理聊天会话（{@code chat_session}）和消息（{@code chat_message}）在 MySQL 中的持久化。</li>
 *   <li>异步发送消息时将 AI 任务投入 RabbitMQ 队列并立即返回，由后台 Worker 异步处理。</li>
 *   <li>流式发送消息时在工作线程中调用 FastAPI SSE 接口，实时将事件推送给前端并在流结束后持久化回答。</li>
 * </ol>
 */
@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    /** 日志中时间字段的格式化器。 */
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    /** 异步模式下返回给前端的占位回答。 */
    private static final String QUEUED_ANSWER = "AI 任务已提交，正在异步处理中。";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiTaskPublisher aiTaskPublisher;
    private final AgentClient agentClient;
    /** 流式 SSE 转发的工作线程池，避免阻塞 Servlet 请求线程。 */
    private final ExecutorService streamingExecutor = Executors.newCachedThreadPool();

    public ChatServiceImpl(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            AiTaskPublisher aiTaskPublisher,
            AgentClient agentClient
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.aiTaskPublisher = aiTaskPublisher;
        this.agentClient = agentClient;
    }

    @Override
    @Transactional
    public SendMessageResponse sendMessage(SendMessageRequest request) {
        long startedAt = System.currentTimeMillis();
        // 复用已有会话或新建会话。
        ChatSessionEntity session = resolveSession(request);

        int nextSeq = (int) chatMessageRepository.countBySessionIdAndIsDeleted(session.getId(), 0) + 1;
        ChatMessageEntity userMessage = buildUserMessage(request, session, nextSeq);
        userMessage = chatMessageRepository.save(userMessage);

        // 如果是新会话且标题还是默认值，设置标题为用户的第一个提问
        if (nextSeq == 1 && (session.getTitle() == null || session.getTitle().equals("新对话"))) {
            session.setTitle(request.getContent());
        }

        // 异步模式：将 AI 任务投入消息队列，立即返回给调用方。
        aiTaskPublisher.publish(session.getSessionNo(), request.getUserId(), request.getContent());

        session.setLastMessageAt(LocalDateTime.now());
        session.setCurrentAgent("router");
        session.setSessionStatus("PROCESSING");
        chatSessionRepository.save(session);

        log.info(
            "chat_message_queued sessionNo={} userId={} sceneType={} messageLen={} latencyMs={}",
            session.getSessionNo(),
            request.getUserId(),
            session.getSceneType(),
            request.getContent() == null ? 0 : request.getContent().length(),
            System.currentTimeMillis() - startedAt
        );

        SendMessageResponse response = new SendMessageResponse();
        response.setSessionNo(session.getSessionNo());
        response.setUserMessageId(userMessage.getId());
        response.setAgentMessageId(null);
        response.setAnswer(QUEUED_ANSWER);
        response.setTaskStatus("QUEUED");
        return response;
    }

    @Override
    @Transactional
    public SseEmitter streamMessage(SendMessageRequest request) {
        long startedAt = System.currentTimeMillis();
        ChatSessionEntity session = resolveSession(request);

        int nextSeq = (int) chatMessageRepository.countBySessionIdAndIsDeleted(session.getId(), 0) + 1;
        ChatMessageEntity userMessage = buildUserMessage(request, session, nextSeq);
        chatMessageRepository.save(userMessage);

        // 如果是新会话且标题还是默认值，设置标题为用户的第一个提问
        if (nextSeq == 1 && (session.getTitle() == null || session.getTitle().equals("新对话"))) {
            session.setTitle(request.getContent());
        }

        session.setLastMessageAt(LocalDateTime.now());
        session.setCurrentAgent("router");
        session.setSessionStatus("PROCESSING");
        chatSessionRepository.save(session);

        SseEmitter emitter = new SseEmitter(0L);
        log.info(
            "chat_stream_requested sessionNo={} userId={} sceneType={} messageLen={} latencyMs={}",
            session.getSessionNo(),
            request.getUserId(),
            session.getSceneType(),
            request.getContent() == null ? 0 : request.getContent().length(),
            System.currentTimeMillis() - startedAt
        );
        // 在工作线程中执行 SSE 转发，避免阻塞 Servlet 请求线程。
        streamingExecutor.execute(() -> streamAgentResponse(emitter, request, session, nextSeq + 1));
        return emitter;
    }

    @Override
    @Transactional(readOnly = true)
    public ChatHistoryResponse getChatHistory(String sessionNo, int page, int pageSize) {
        ChatSessionEntity session = chatSessionRepository.findBySessionNoAndIsDeleted(sessionNo, 0)
                .orElseThrow(() -> new IllegalArgumentException("Session not found"));

        int safePage = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);

        Page<ChatMessageEntity> msgPage = chatMessageRepository.findBySessionIdAndIsDeletedOrderBySequenceNoAsc(
                session.getId(),
                0,
                PageRequest.of(safePage - 1, safePageSize)
        );

        List<ChatMessageItemResponse> items = msgPage.getContent().stream()
                .map(this::toItem)
                .collect(Collectors.toList());

        ChatHistoryResponse response = new ChatHistoryResponse();
        response.setSessionNo(sessionNo);
        response.setPage(safePage);
        response.setPageSize(safePageSize);
        response.setTotal(msgPage.getTotalElements());
        response.setItems(items);
        return response;
    }

    /**
     * 复用已有会话或在没有提供 sessionNo 时新建会话。
     * 新会话会持久化到数据库并以默认 router Agent 和 ACTIVE 状态启动。
     */
    private ChatSessionEntity resolveSession(SendMessageRequest request) {
        if (request.getSessionNo() != null && !request.getSessionNo().isBlank()) {
            return chatSessionRepository.findBySessionNoAndIsDeleted(request.getSessionNo(), 0)
                    .orElseThrow(() -> new IllegalArgumentException("Session not found"));
        }

        ChatSessionEntity session = new ChatSessionEntity();
        session.setSessionNo(generateSessionNo());
        session.setUserId(request.getUserId());
        session.setTitle(request.getTitle());
        session.setSceneType(request.getSceneType() == null || request.getSceneType().isBlank()
                ? "mixed"
                : request.getSceneType());
        session.setCurrentAgent("router");
        return chatSessionRepository.save(session);
    }

    private ChatMessageEntity buildUserMessage(SendMessageRequest request, ChatSessionEntity session, int sequenceNo) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setMessageNo(generateMessageNo());
        entity.setSessionId(session.getId());
        entity.setUserId(request.getUserId());
        entity.setSenderType("USER");
        entity.setMessageType(request.getMessageType() == null || request.getMessageType().isBlank()
                ? "TEXT"
                : request.getMessageType());
        entity.setSequenceNo(sequenceNo);
        entity.setContent(request.getContent());
        return entity;
    }

    private ChatMessageEntity buildAgentMessage(
            ChatSessionEntity session,
            int sequenceNo,
            String content,
            String agentType
    ) {
        ChatMessageEntity entity = new ChatMessageEntity();
        entity.setMessageNo(generateMessageNo());
        entity.setSessionId(session.getId());
        entity.setUserId(session.getUserId());
        entity.setSenderType("AGENT");
        entity.setAgentType(agentType == null || agentType.isBlank() ? "router" : agentType);
        entity.setMessageType("TEXT");
        entity.setSequenceNo(sequenceNo);
        entity.setContent(content);
        return entity;
    }

    private ChatMessageItemResponse toItem(ChatMessageEntity msg) {
        ChatMessageItemResponse item = new ChatMessageItemResponse();
        item.setMessageId(msg.getId());
        item.setMessageNo(msg.getMessageNo());
        item.setSenderType(msg.getSenderType());
        item.setAgentType(msg.getAgentType());
        item.setMessageType(msg.getMessageType());
        item.setSequenceNo(msg.getSequenceNo());
        item.setContent(msg.getContent());
        item.setSentAt(msg.getSentAt() == null ? null : msg.getSentAt().format(TS_FMT));
        return item;
    }

    /**
     * 在工作线程中调用 FastAPI SSE 接口，逐帧推送事件给前端，
     * 流结束后将完整回答和 Agent 类型持久化到数据库。
     */
    private void streamAgentResponse(
            SseEmitter emitter,
            SendMessageRequest request,
            ChatSessionEntity session,
            int agentSequenceNo
    ) {
        long startedAt = System.currentTimeMillis();
        // 用于拼接增量文本片段，最终得到完整回答。
        StringBuilder answerBuilder = new StringBuilder();
        String[] agentUsedHolder = new String[]{"router"};

        try {
            agentClient.streamChat(
                    session.getSessionNo(),
                    request.getUserId(),
                    request.getContent(),
                    event -> handleAgentStreamEvent(emitter, event, answerBuilder, agentUsedHolder)
            );

            String finalAnswer = answerBuilder.toString().trim();
            if (finalAnswer.isEmpty()) {
                throw new IllegalStateException("Agent stream completed without answer content");
            }

            // 流结束后一次性持久化 Agent 回复消息到数据库。
            ChatMessageEntity agentMessage = buildAgentMessage(
                    session,
                    agentSequenceNo,
                    finalAnswer,
                    agentUsedHolder[0]
            );
            chatMessageRepository.save(agentMessage);

            // 同步更新会话的当前 Agent 和最后消息时间。
            session.setCurrentAgent(agentUsedHolder[0]);
            session.setSessionStatus("ACTIVE");
            session.setLastMessageAt(LocalDateTime.now());
            chatSessionRepository.save(session);
                log.info(
                    "chat_stream_completed sessionNo={} userId={} agent={} answerLen={} latencyMs={}",
                    session.getSessionNo(),
                    request.getUserId(),
                    agentUsedHolder[0],
                    finalAnswer.length(),
                    System.currentTimeMillis() - startedAt
                );
            emitter.complete();
        } catch (Exception ex) {
            session.setSessionStatus("FAILED");
            session.setLastMessageAt(LocalDateTime.now());
            chatSessionRepository.save(session);
                log.error(
                    "chat_stream_failed sessionNo={} userId={} error={}",
                    session.getSessionNo(),
                    request.getUserId(),
                    ex.getMessage(),
                    ex
                );
            try {
                emitter.send(SseEmitter.event()
                        .name("error")
                        .data("{\"message\":\"流式回答失败，请稍后重试。\"}", MediaType.APPLICATION_JSON));
            } catch (Exception ignored) {
            }
            emitter.complete();
        }
    }

    /**
     * 处理单个 SSE 事件，将内容追加到 answerBuilder 并转发给前端。
     * <ul>
     *   <li>{@code chunk} 事件：追加 {@code data.content} 的增量片段。</li>
     *   <li>{@code done}  事件：用 {@code data.answer} 替换整个 answerBuilder（权威最终答案）。</li>
     * </ul>
     */
    private void handleAgentStreamEvent(
            SseEmitter emitter,
            AgentStreamEvent event,
            StringBuilder answerBuilder,
            String[] agentUsedHolder
    ) {
        try {
            JsonNode data = event.getData();
            // 记录 agent_used 字段，流结束后用于持久化消息的 agentType。
            if (data != null && data.hasNonNull("agent_used")) {
                agentUsedHolder[0] = data.get("agent_used").asText();
            }
            // chunk 事件：追加增量内容片段。
            if ("chunk".equals(event.getEvent()) && data != null && data.hasNonNull("content")) {
                answerBuilder.append(data.get("content").asText());
            }
            // done 事件：data.answer 是 FastAPI 侧的完整最终回答，直接替换。
            if ("done".equals(event.getEvent()) && data != null && data.hasNonNull("answer")) {
                answerBuilder.setLength(0);
                answerBuilder.append(data.get("answer").asText());
            }
            emitter.send(SseEmitter.event().name(event.getEvent()).data(data == null ? "{}" : data.toString(), MediaType.APPLICATION_JSON));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to forward stream event", ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ChatSessionListResponse listSessions(Long userId, int page, int pageSize) {
        int safePage     = Math.max(page, 1);
        int safePageSize = Math.min(Math.max(pageSize, 1), 100);

        Page<ChatSessionEntity> sessionPage = chatSessionRepository
                .findByUserIdAndIsDeletedOrderByLastMessageAtDesc(
                        userId, 0, PageRequest.of(safePage - 1, safePageSize));

        List<ChatSessionItemResponse> items = sessionPage.getContent().stream()
                .map(this::toSessionItem)
                .collect(Collectors.toList());

        ChatSessionListResponse response = new ChatSessionListResponse();
        response.setPage(safePage);
        response.setPageSize(safePageSize);
        response.setTotal(sessionPage.getTotalElements());
        response.setItems(items);
        return response;
    }

    @Override
    @Transactional
    public ChatSessionItemResponse createSession(CreateSessionRequest request) {
        ChatSessionEntity session = new ChatSessionEntity();
        session.setSessionNo(generateSessionNo());
        session.setUserId(request.getUserId());
        session.setTitle(request.getTitle() == null || request.getTitle().isBlank()
                ? "新对话" : request.getTitle());
        session.setSceneType(request.getSceneType() == null || request.getSceneType().isBlank()
                ? "mixed" : request.getSceneType());
        session.setCurrentAgent("router");
        session = chatSessionRepository.save(session);
        return toSessionItem(session);
    }

    private ChatSessionItemResponse toSessionItem(ChatSessionEntity session) {
        ChatSessionItemResponse item = new ChatSessionItemResponse();
        item.setSessionNo(session.getSessionNo());
        item.setTitle(session.getTitle());
        item.setSceneType(session.getSceneType());
        item.setSessionStatus(session.getSessionStatus());
        item.setLastMessageAt(session.getLastMessageAt() == null ? null
                : session.getLastMessageAt().format(TS_FMT));
        item.setCreatedAt(session.getCreatedAt() == null ? null
                : session.getCreatedAt().format(TS_FMT));
        return item;
    }

    private String generateSessionNo() {
        return "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
    }

    private String generateMessageNo() {
        return "M" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
    }
}