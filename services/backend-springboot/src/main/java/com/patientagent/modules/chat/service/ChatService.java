package com.patientagent.modules.chat.service;

import com.patientagent.modules.chat.dto.ChatHistoryResponse;
import com.patientagent.modules.chat.dto.SendMessageRequest;
import com.patientagent.modules.chat.dto.SendMessageResponse;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * 聊天业务服务接口，定义三类核心操作：
 * <ul>
 *   <li>{@link #sendMessage} — 异步发送消息，任务投入 RabbitMQ 队列后立即返回。</li>
 *   <li>{@link #streamMessage} — 流式发送消息，通过 SSE 实时推送 Agent 回答给前端。</li>
 *   <li>{@link #getChatHistory} — 分页查询指定会话的历史消息记录。</li>
 * </ul>
 */
public interface ChatService {

    /**
     * 异步发送消息（RabbitMQ 模式）。
     * 保存用户消息后将 AI 任务投入消息队列，立即返回「任务已提交」状态，
     * AI 回答由消费者异步写回数据库。
     *
     * @param request 包含用户 ID、会话编号、消息内容等
     * @return 包含会话编号、消息 ID 和任务状态的响应
     */
    SendMessageResponse sendMessage(SendMessageRequest request);

    /**
     * 流式发送消息（SSE 模式）。
     * 在工作线程中调用 FastAPI 流式接口，通过 {@link SseEmitter} 实时把
     * chunk / done / error 事件推送给前端，流结束后将完整回答持久化到数据库。
     *
     * @param request 包含用户 ID、会话编号、消息内容等
     * @return 用于向客户端推送 SSE 事件的 {@link SseEmitter}
     */
    SseEmitter streamMessage(SendMessageRequest request);

    /**
     * 分页查询会话历史消息，按消息序号升序排列。
     *
     * @param sessionNo 会话编号
     * @param page      页码（从 1 开始）
     * @param pageSize  每页条数（最大 100）
     * @return 包含消息列表和分页信息的响应
     */
    ChatHistoryResponse getChatHistory(String sessionNo, int page, int pageSize);
}
