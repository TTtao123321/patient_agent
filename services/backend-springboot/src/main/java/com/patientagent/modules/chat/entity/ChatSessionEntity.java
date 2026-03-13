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
 * 聊天会话数据库实体，对应 {@code chat_session} 表。
 * <p>
 * 每个会话代表用户与 AI 的一段完整对话过程。
 * {@code sceneType} 决定了路由到哪个 Agent，{@code currentAgent} 记录当前最新一次处理该会话的 Agent。
 * </p>
 */
@Entity
@Table(name = "chat_session")
public class ChatSessionEntity {

    /** 主键，数据库自增。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 会话业务编号，全局唯一，格式为 {@code S + 31位十六进制}。 */
    @Column(name = "session_no", nullable = false, unique = true, length = 32)
    private String sessionNo;

    /** 会话所属用户 ID。 */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** 关联的病历模块 ID（可为空）。 */
    @Column(name = "medical_record_id")
    private Long medicalRecordId;

    /** 会话标题，前端展示用。 */
    @Column(name = "title", length = 128)
    private String title;

    /** 场景类型，如 mixed / report_analysis，影响 Agent 路由逻辑。 */
    @Column(name = "scene_type", nullable = false, length = 32)
    private String sceneType;

    /** 当前处理会话的 Agent 类型，随每次回复更新。 */
    @Column(name = "current_agent", length = 32)
    private String currentAgent;

    /** 会话状态：{@code ACTIVE} / {@code PROCESSING} / {@code FAILED}。 */
    @Column(name = "session_status", nullable = false, length = 16)
    private String sessionStatus;

    /** AI 生成的会话摘要（暂未实现，预留序。） */
    @Column(name = "summary")
    private String summary;

    /** 会话开始时间。 */
    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt;

    /** 最近一条消息的发送时间，用于会话列表排序。 */
    @Column(name = "last_message_at")
    private LocalDateTime lastMessageAt;

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
        this.startedAt = this.startedAt == null ? now : this.startedAt;
        this.lastMessageAt = this.lastMessageAt == null ? now : this.lastMessageAt;
        this.createdAt = now;
        this.updatedAt = now;
        this.sessionStatus = this.sessionStatus == null ? "ACTIVE" : this.sessionStatus;
        this.sceneType = this.sceneType == null ? "mixed" : this.sceneType;
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

    public Long getMedicalRecordId() {
        return medicalRecordId;
    }

    public void setMedicalRecordId(Long medicalRecordId) {
        this.medicalRecordId = medicalRecordId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getCurrentAgent() {
        return currentAgent;
    }

    public void setCurrentAgent(String currentAgent) {
        this.currentAgent = currentAgent;
    }

    public String getSessionStatus() {
        return sessionStatus;
    }

    public void setSessionStatus(String sessionStatus) {
        this.sessionStatus = sessionStatus;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public LocalDateTime getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(LocalDateTime startedAt) {
        this.startedAt = startedAt;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public void setLastMessageAt(LocalDateTime lastMessageAt) {
        this.lastMessageAt = lastMessageAt;
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
