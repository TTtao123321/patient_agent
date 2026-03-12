package com.patientagent.modules.chat.service;

import com.patientagent.modules.chat.dto.ChatHistoryResponse;
import com.patientagent.modules.chat.dto.SendMessageRequest;
import com.patientagent.modules.chat.dto.SendMessageResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface ChatService {

    SendMessageResponse sendMessage(SendMessageRequest request);

    SseEmitter streamMessage(SendMessageRequest request);

    ChatHistoryResponse getChatHistory(String sessionNo, int page, int pageSize);
}
