package com.patientagent.modules.chat.controller;

import com.patientagent.common.response.ApiResponse;
import com.patientagent.modules.chat.dto.ChatHistoryResponse;
import com.patientagent.modules.chat.dto.ChatSessionItemResponse;
import com.patientagent.modules.chat.dto.ChatSessionListResponse;
import com.patientagent.modules.chat.dto.CreateSessionRequest;
import com.patientagent.modules.chat.dto.SendMessageRequest;
import com.patientagent.modules.chat.dto.SendMessageResponse;
import com.patientagent.modules.chat.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

/**
 * 聊天模块 HTTP 入口：
 * <ul>
 *   <li>GET  /api/v1/chat/sessions              — 查询用户会话列表</li>
 *   <li>POST /api/v1/chat/sessions              — 创建新会话（不发消息）</li>
 *   <li>POST /api/v1/chat/messages/send         — 异步发送消息</li>
 *   <li>POST /api/v1/chat/messages/stream       — 流式发送消息（SSE）</li>
 *   <li>GET  /api/v1/chat/sessions/{no}/messages — 分页查询历史消息</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /** 查询用户会话列表，按最新消息时间倒序。 */
    @GetMapping("/sessions")
    public ApiResponse<ChatSessionListResponse> listSessions(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, chatService.listSessions(userId, page, pageSize));
    }

    /** 创建新会话（不发任何消息，仅初始化会话记录）。 */
    @PostMapping("/sessions")
    public ApiResponse<ChatSessionItemResponse> createSession(
            @Valid @RequestBody CreateSessionRequest request
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, chatService.createSession(request));
    }

    /** 异步发送消息，立即返回 QUEUED 状态，AI 由后台处理。 */
    @PostMapping("/messages/send")
    public ApiResponse<SendMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, chatService.sendMessage(request));
    }

    /** 流式发送消息，返回 SSE 流。 */
    @PostMapping(path = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@Valid @RequestBody SendMessageRequest request) {
        return chatService.streamMessage(request);
    }

    /** 分页查询会话历史消息，按序列号升序。 */
    @GetMapping("/sessions/{sessionNo}/messages")
    public ApiResponse<ChatHistoryResponse> getHistory(
            @PathVariable String sessionNo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int pageSize
    ) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, chatService.getChatHistory(sessionNo, page, pageSize));
    }
}
