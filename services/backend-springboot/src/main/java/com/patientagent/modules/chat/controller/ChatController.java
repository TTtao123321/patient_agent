package com.patientagent.modules.chat.controller;

import com.patientagent.common.response.ApiResponse;
import com.patientagent.modules.chat.dto.ChatHistoryResponse;
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
 * 聊天模块 HTTP 入口，提供三个接口：
 * <ul>
 *   <li>{@code POST /api/v1/chat/messages/send} — 异步发送消息，返回任务状态。</li>
 *   <li>{@code POST /api/v1/chat/messages/stream} — 流式发送消息，返回 SSE 流。</li>
 *   <li>{@code GET /api/v1/chat/sessions/{sessionNo}/messages} — 分页查询历史消息。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * 异步发送消息。
     * 用户消息存入数据库后将 AI 任务投入 RabbitMQ 队列，立即返回「任务已提交」状态。
     */
    @PostMapping("/messages/send")
    public ApiResponse<SendMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, chatService.sendMessage(request));
    }

    /**
     * 流式发送消息。
     * 直接将 FastAPI SSE 流转发给前端，流结束后自动将完整回答写入数据库。
     */
    @PostMapping(path = "/messages/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamMessage(@Valid @RequestBody SendMessageRequest request) {
        return chatService.streamMessage(request);
    }

    /**
     * 分页查询会话的历史消息，按序列号升序返回。
     *
     * @param sessionNo 会话编号
     * @param page      页码，默认 1
     * @param pageSize  每页条数，默认 50
     */
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
