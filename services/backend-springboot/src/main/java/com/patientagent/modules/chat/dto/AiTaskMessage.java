package com.patientagent.modules.chat.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AiTaskMessage {

    @JsonProperty("session_id")
    private String sessionId;

    @JsonProperty("user_id")
    private Long userId;

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