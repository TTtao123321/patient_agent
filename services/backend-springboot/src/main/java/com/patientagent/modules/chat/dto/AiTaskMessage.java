package com.patientagent.modules.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 发往 RabbitMQ 队列的 AI 处理任务消息实体。
 * <p>
 * 当用户通过「异步发送」接口发起对话时，该对象会被序列化为 JSON 并投入
 * {@code chat.task.queue} 队列，由后台 Worker 进行异步 AI 调用。
 * </p>
 */
public class AiTaskMessage {

    /** 会话编号（sessionNo），用于关联回复消息到对应会话。 */
    @JsonProperty("session_id")
    private String sessionId;

    /** 用户 ID。 */
    @JsonProperty("user_id")
    private Long userId;

    /** 用户发送的原始消息内容。 */
    @JsonProperty("message")
    private String message;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}