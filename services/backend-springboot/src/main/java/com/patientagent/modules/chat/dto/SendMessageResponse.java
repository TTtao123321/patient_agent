package com.patientagent.modules.chat.dto;

/**
 * 发送消息接口的响应 DTO。
 * <p>
 * 异步模式下 {@code agentMessageId} 为空，{@code taskStatus} 为 {@code QUEUED}；
 * 同步模式下 {@code answer} 携带完整回答，{@code taskStatus} 为 {@code DONE}。
 * </p>
 */
public class SendMessageResponse {

    /** 消息所属会话编号。 */
    private String sessionNo;
    /** 用户消息主键 ID。 */
    private Long userMessageId;
    /** Agent 回复消息主键 ID，异步模式下为 null。 */
    private Long agentMessageId;
    /** Agent 回答文本（同步模式）或提示词（异步模式）。 */
    private String answer;
    /** 任务状态：{@code QUEUED}（异步排队）或 {@code DONE}（已完成）。 */
    private String taskStatus;

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

    public String getTaskStatus() {
        return taskStatus;
    }

    public void setTaskStatus(String taskStatus) {
        this.taskStatus = taskStatus;
    }
}
