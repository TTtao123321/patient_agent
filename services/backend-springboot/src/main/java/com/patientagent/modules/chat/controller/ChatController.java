package com.patientagent.modules.chat.controller;

import com.patientagent.common.response.ApiResponse;
import com.patientagent.modules.chat.dto.ChatHistoryResponse;
import com.patientagent.modules.chat.dto.SendMessageRequest;
import com.patientagent.modules.chat.dto.SendMessageResponse;
import com.patientagent.modules.chat.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping("/messages/send")
    public ApiResponse<SendMessageResponse> sendMessage(@Valid @RequestBody SendMessageRequest request) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, chatService.sendMessage(request));
    }

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
