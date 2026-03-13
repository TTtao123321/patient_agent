package com.patientagent.client.agent;

import com.patientagent.client.agent.dto.AgentStreamEvent;

import java.util.function.Consumer;

/**
 * AI Agent 调用接口，定义与 FastAPI Agent 服务通信的两种模式：
 * <ul>
 *   <li>{@link #chat} — 同步请求，阻塞等待完整回答后返回字符串。</li>
 *   <li>{@link #streamChat} — SSE 流式请求，通过回调逐帧推送 Agent 事件。</li>
 * </ul>
 */
public interface AgentClient {

    /**
     * 同步发起对话，阻塞等待 Agent 服务返回完整回答后返回。
     *
     * @param sessionNo 会话编号，用于 Agent 侧维护上下文
     * @param userId    当前用户 ID
     * @param message   用户提问内容
     * @return Agent 生成的完整回答文本
     */
    String chat(String sessionNo, Long userId, String message);

    /**
     * 流式发起对话，通过 {@code eventConsumer} 回调逐帧推送 SSE 事件。
     * 调用方需要在回调中处理 chunk / done / error 等事件类型。
     *
     * @param sessionNo     会话编号
     * @param userId        当前用户 ID
     * @param message       用户提问内容
     * @param eventConsumer SSE 事件回调，每收到一帧数据即被调用一次
     */
    void streamChat(String sessionNo, Long userId, String message, Consumer<AgentStreamEvent> eventConsumer);
}
