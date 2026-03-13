package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 发送给 FastAPI Agent 服务的请求实体。
 * <p>
 * 字段需要与 Python 层的字段名对齐，通过 {@code @JsonProperty} 做 snake_case 转换。
 * </p>
 */
public class AgentChatRequest {

    /** 会话编号，用于 Agent 在服务端维护对话上下文。 */
    @JsonProperty("session_no")
    private String sessionNo;

    /** 用户 ID，Agent 可用于权限隔离和个性化回答。 */
    @JsonProperty("user_id")
    private Long userId;

    /** 用户输入的提问内容。 */
    @JsonProperty("query")
    private String query;

    public String getSessionNo() {
        return sessionNo;
    }

    public void setSessionNo(String sessionNo) {
        this.sessionNo = sessionNo;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getQuery() {
        return query;
    }

    public void setQuery(String query) {
        this.query = query;
    }
}
