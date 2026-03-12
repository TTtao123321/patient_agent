package com.patientagent.modules.chat.service;

import com.patientagent.modules.chat.dto.ChatHistoryResponse;
import com.patientagent.modules.chat.dto.SendMessageRequest;
import com.patientagent.modules.chat.dto.SendMessageResponse;

public interface ChatService {

    SendMessageResponse sendMessage(SendMessageRequest request);

    ChatHistoryResponse getChatHistory(String sessionNo, int page, int pageSize);
}
