package com.patientagent.modules.chat.service;

import com.patientagent.modules.chat.dto.ChatHistoryResponse;
import com.patientagent.modules.chat.dto.ChatMessageItemResponse;
import com.patientagent.modules.chat.dto.SendMessageRequest;
import com.patientagent.modules.chat.dto.SendMessageResponse;
import com.patientagent.modules.chat.entity.ChatMessageEntity;
import com.patientagent.modules.chat.entity.ChatSessionEntity;
import com.patientagent.modules.chat.repository.ChatMessageRepository;
import com.patientagent.modules.chat.repository.ChatSessionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ChatServiceImpl implements ChatService {

    private static final DateTimeFormatter TS_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final String QUEUED_ANSWER = "AI 任务已提交，正在异步处理中。";

    private final ChatSessionRepository chatSessionRepository;
    private final ChatMessageRepository chatMessageRepository;
    private final AiTaskPublisher aiTaskPublisher;

    public ChatServiceImpl(
            ChatSessionRepository chatSessionRepository,
            ChatMessageRepository chatMessageRepository,
            AiTaskPublisher aiTaskPublisher
    ) {
        this.chatSessionRepository = chatSessionRepository;
        this.chatMessageRepository = chatMessageRepository;
        this.aiTaskPublisher = aiTaskPublisher;
    }

    @Override
    @Transactional
    public SendMessageResponse sendMessage(SendMessageRequest request) {
        ChatSessionEntity session = resolveSession(request);

        int nextSeq = (int) chatMessageRepository.countBySessionIdAndIsDeleted(session.getId(), 0) + 1;
        ChatMessageEntity userMessage = buildUserMessage(request, session, nextSeq);
        userMessage = chatMessageRepository.save(userMessage);

        aiTaskPublisher.publish(session.getSessionNo(), request.getUserId(), request.getContent());

        session.setLastMessageAt(LocalDateTime.now());
        session.setCurrentAgent("router");
        session.setSessionStatus("PROCESSING");
        chatSessionRepository.save(session);

        SendMessageResponse response = new SendMessageResponse();
        response.setSessionNo(session.getSessionNo());
        response.setUserMessageId(userMessage.getId());
        response.setAgentMessageId(null);
        response.setAnswer(QUEUED_ANSWER);
        response.setTaskStatus("QUEUED");
        return response;
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

    private String generateSessionNo() {
        return "S" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
    }

    private String generateMessageNo() {
        return "M" + UUID.randomUUID().toString().replace("-", "").substring(0, 31);
    }
}
