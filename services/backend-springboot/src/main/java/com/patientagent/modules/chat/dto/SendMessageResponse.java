package com.patientagent.modules.chat.dto;

public class SendMessageResponse {

    private String sessionNo;
    private Long userMessageId;
    private Long agentMessageId;
    private String answer;

    public String getSessionNo() {
        return sessionNo;
    }

    public void setSessionNo(String sessionNo) {
        this.sessionNo = sessionNo;
    }

    public Long getUserMessageId() {
        return userMessageId;
    }

    public void setUserMessageId(Long userMessageId) {
        this.userMessageId = userMessageId;
    }

    public Long getAgentMessageId() {
        return agentMessageId;
    }

    public void setAgentMessageId(Long agentMessageId) {
        this.agentMessageId = agentMessageId;
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }
}
