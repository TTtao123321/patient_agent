package com.patientagent.client.agent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AgentChatRequest {

    @JsonProperty("session_no")
    private String sessionNo;

    @JsonProperty("user_id")
    private Long userId;

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
