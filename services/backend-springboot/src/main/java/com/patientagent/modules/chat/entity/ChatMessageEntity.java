package com.patientagent.modules.chat.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 聊天消息数据库实体，对应 {@code chat_message} 表。
 * <p>
 * 每条记录代表会话中的一条消息，可能是用户发送的（{@code senderType=USER}）
 * 或 AI Agent 回复的（{@code senderType=AGENT}）。
 * 易删除通过 {@code is_deleted} 字段实现软删除。
 * </p>
 */
@Entity
@Table(name = "chat_message")
public class ChatMessageEntity {

    /** 主键，数据库自增。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 消息业务编号，全局唯一。 */
    @Column(name = "message_no", nullable = false, unique = true, length = 32)
    private String messageNo;

    /** 所属会话的数据库主键 ID。 */
    @Column(name = "session_id", nullable = false)
    private Long sessionId;

    /** 这条消息关联的用户 ID。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 关联的医学报告 ID（可为空）。 */
    @Column(name = "report_id")
    private Long reportId;

    /** 发送方类型：{@code USER} 或 {@code AGENT}。 */
    @Column(name = "sender_type", nullable = false, length = 16)
    private String senderType;

    /** 处理该消息的 Agent 类型（如 router / report_analysis），用户消息可为空。 */
    @Column(name = "agent_type", length = 32)
    private String agentType;

    /** 消息类型：{@code TEXT} / {@code IMAGE} 等。 */
    @Column(name = "message_type", nullable = false, length = 16)
    private String messageType;

    /** 消息在会话中的序列号，用于按顺序排列展示。 */
    @Column(name = "sequence_no", nullable = false)
    private Integer sequenceNo;

    /** 消息文本内容。 */
    @Column(name = "content", nullable = false)
    private String content;

    /** 结构化载荷（JSON 字符串），如报告解析结果等复杂输出。 */
    @Column(name = "structured_payload", columnDefinition = "json")
    private String structuredPayload;

    /** 引用知识库来源的 JSON 列表（可为空）。 */
    @Column(name = "citations", columnDefinition = "json")
    private String citations;

    /** 输入 Token 计数（可为空）。 */
    @Column(name = "tokens_in")
    private Integer tokensIn;

    /** 输出 Token 计数（可为空）。 */
    @Column(name = "tokens_out")
    private Integer tokensOut;

    /** 消息发送时间。 */
    @Column(name = "sent_at", nullable = false)
    private LocalDateTime sentAt;

    /** 记录创建时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 记录最后更新时间。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 软删除标志：0 = 正常，1 = 已删除。 */
    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted;

    /** JPA 持久化前调用，自动设置默认字段。 */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.sentAt = this.sentAt == null ? now : this.sentAt;
        this.createdAt = now;
        this.updatedAt = now;
        this.messageType = this.messageType == null ? "TEXT" : this.messageType;
        this.senderType = this.senderType == null ? "USER" : this.senderType;
        this.isDeleted = this.isDeleted == null ? 0 : this.isDeleted;
    }

    /** JPA 更新前调用，自动刷新 {@code updatedAt} 字段。 */
    @PreUpdate
    public void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getMessageNo() {
        return messageNo;
    }

    public void setMessageNo(String messageNo) {
        this.messageNo = messageNo;
    }

    public Long getSessionId() {
        return sessionId;
    }

    public void setSessionId(Long sessionId) {
        this.sessionId = sessionId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getReportId() {
        return reportId;
    }

    public void setReportId(Long reportId) {
        this.reportId = reportId;
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

    public String getStructuredPayload() {
        return structuredPayload;
    }

    public void setStructuredPayload(String structuredPayload) {
        this.structuredPayload = structuredPayload;
    }

    public String getCitations() {
        return citations;
    }

    public void setCitations(String citations) {
        this.citations = citations;
    }

    public Integer getTokensIn() {
        return tokensIn;
    }

    public void setTokensIn(Integer tokensIn) {
        this.tokensIn = tokensIn;
    }

    public Integer getTokensOut() {
        return tokensOut;
    }

    public void setTokensOut(Integer tokensOut) {
        this.tokensOut = tokensOut;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getIsDeleted() {
        return isDeleted;
    }

    public void setIsDeleted(Integer isDeleted) {
        this.isDeleted = isDeleted;
    }
}
