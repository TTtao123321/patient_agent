package com.patientagent.modules.chat.dto;

/**
 * 会话列表单条记录，用于前端会话 Sidebar 展示。
 */
public class ChatSessionItemResponse {

    /** 会话业务编号。 */
    private String sessionNo;
    /** 会话标题。 */
    private String title;
    /** 场景类型：mixed / report_analysis 等。 */
    private String sceneType;
    /** 会话状态：ACTIVE / PROCESSING / FAILED。 */
    private String sessionStatus;
    /** 最近一条消息时间，格式 yyyy-MM-dd HH:mm:ss。 */
    private String lastMessageAt;
    /** 会话创建时间，格式 yyyy-MM-dd HH:mm:ss。 */
    private String createdAt;

    public String getSessionNo() { return sessionNo; }
    public void setSessionNo(String sessionNo) { this.sessionNo = sessionNo; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSceneType() { return sceneType; }
    public void setSceneType(String sceneType) { this.sceneType = sceneType; }

    public String getSessionStatus() { return sessionStatus; }
    public void setSessionStatus(String sessionStatus) { this.sessionStatus = sessionStatus; }

    public String getLastMessageAt() { return lastMessageAt; }
    public void setLastMessageAt(String lastMessageAt) { this.lastMessageAt = lastMessageAt; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
}
