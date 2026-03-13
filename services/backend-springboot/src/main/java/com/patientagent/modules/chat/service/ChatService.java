package com.patientagent.modules.chat.service;

import com.patientagent.modules.chat.dto.ChatHistoryResponse;
import com.patientagent.modules.chat.dto.ChatSessionItemResponse;
import com.patientagent.modules.chat.dto.ChatSessionListResponse;
import com.patientagent.modules.chat.dto.CreateSessionRequest;
import com.patientagent.modules.chat.dto.SendMessageRequest;
import com.patientagent.modules.chat.dto.SendMessageResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天业务服务接口。
 */
public interface ChatService {

    SendMessageResponse sendMessage(SendMessageRequest request);

    SseEmitter streamMessage(SendMessageRequest request);

    ChatHistoryResponse getChatHistory(String sessionNo, int page, int pageSize);

    /**
     * 查询指定用户的会话列表，按最新消息时间倒序分页。
     */
    ChatSessionListResponse listSessions(Long userId, int page, int pageSize);

    /**
     * 创建空会话（不发送任何消息），返回新会话信息。
     */
    ChatSessionItemResponse createSession(CreateSessionRequest request);
}
