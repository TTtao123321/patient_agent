package com.patientagent.modules.chat.dto;

/**
 * 展示单条聊天消息的响应 DTO。
 * <p>
 * 起到安全层隔离作用：只将前端需要的字段暴露出来，隐藏数据库实体中的内部字段。
 * </p>
 */
public class ChatMessageItemResponse {

    /** 消息主键 ID（数据库自增）。 */
    private Long messageId;
    /** 消息业务编号（全局唯一）。 */
    private String messageNo;
    /** 发送方类型：{@code USER} 或 {@code AGENT}。 */
    private String senderType;
    /** 处理本条消息的 Agent 类型（如 router / report_analysis 等）。 */
    private String agentType;
    /** 消息类型：{@code TEXT} / {@code IMAGE} 等。 */
    private String messageType;
    /** 消息在会话中的序列号，用于按顺序展示。 */
    private Integer sequenceNo;
    /** 消息文本内容。 */
    private String content;
    /** 发送时间，格式：{@code yyyy-MM-dd HH:mm:ss}。 */
    private String sentAt;

    public Long getMessageId() {
        return messageId;
    }

    public void setMessageId(Long messageId) {
        this.messageId = messageId;
    }

    public String getMessageNo() {
        return messageNo;
    }

    public void setMessageNo(String messageNo) {
        this.messageNo = messageNo;
    }

    public String getSenderType() {
        return senderType;
    }

    public void setSenderType(String senderType) {
        this.senderType = senderType;
    }

    public String getAgentType() {
        return agentType;
    }

    public void setAgentType(String agentType) {
        this.agentType = agentType;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public Integer getSequenceNo() {
        return sequenceNo;
    }

    public void setSequenceNo(Integer sequenceNo) {
        this.sequenceNo = sequenceNo;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSentAt() {
        return sentAt;
    }

    public void setSentAt(String sentAt) {
        this.sentAt = sentAt;
    }
}
