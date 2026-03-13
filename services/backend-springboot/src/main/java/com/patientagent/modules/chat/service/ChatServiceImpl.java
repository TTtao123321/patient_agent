package com.patientagent.modules.chat.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.patientagent.client.agent.AgentClient;
import com.patientagent.client.agent.dto.AgentStreamEvent;
import com.patientagent.modules.chat.dto.ChatHistoryResponse;
import com.patientagent.modules.chat.dto.ChatMessageItemResponse;
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

@Service
public class ChatServiceImpl implements ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatServiceImpl.class);
    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String QUEUED_ANSWER = "AI 任务已提交，正在异步处理中。";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiTaskPublisher aiTaskPublisher;
    private final AgentClient agentClient;
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
        ChatSessionEntity session = resolveSession(request);

        int nextSeq = (int) chatMessageRepository.countBySessionIdAndIsDeleted(session.getId(), 0) + 1;
        ChatMessageEntity userMessage = buildUserMessage(request, session, nextSeq);
        userMessage = chatMessageRepository.save(userMessage);

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

    private void streamAgentResponse(
            SseEmitter emitter,
            SendMessageRequest request,
            ChatSessionEntity session,
            int agentSequenceNo
    ) {
        long startedAt = System.currentTimeMillis();
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

            ChatMessageEntity agentMessage = buildAgentMessage(
                    session,
                    agentSequenceNo,
                    finalAnswer,
                    agentUsedHolder[0]
            );
            chatMessageRepository.save(agentMessage);

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
                        .data("{\"message\":\"流式回答失败，请稍后重试。\"}"));
            } catch (Exception ignored) {
            }
            emitter.completeWithError(ex);
        }
    }

    private void handleAgentStreamEvent(
            SseEmitter emitter,
            AgentStreamEvent event,
            StringBuilder answerBuilder,
            String[] agentUsedHolder
    ) {
        try {
            JsonNode data = event.getData();
            if (data != null && data.hasNonNull("agent_used")) {
                agentUsedHolder[0] = data.get("agent_used").asText();
            }
            if ("chunk".equals(event.getEvent()) && data != null && data.hasNonNull("content")) {
                answerBuilder.append(data.get("content").asText());
            }
            if ("done".equals(event.getEvent()) && data != null && data.hasNonNull("answer")) {
                answerBuilder.setLength(0);
                answerBuilder.append(data.get("answer").asText());
            }
            emitter.send(SseEmitter.event().name(event.getEvent()).data(data == null ? "{}" : data.toString()));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to forward stream event", ex);
        }
    }

    private String generateSessionNo() {
        return "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
    }

    private String generateMessageNo() {
        return "M" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
    }
}
