package com.patientagent.modules.chat.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发送消息请求 DTO，同时适用于异步发送和流式发送两个接口。
 */
public class SendMessageRequest {

    /** 已有会话编号；为空时由服务层自动创建新会话。 */
    private String sessionNo;

    /** 当前用户 ID，必填。 */
    @NotNull
    private Long userId;

    /** 场景类型，如 mixed / report_analysis 等，用于定制 Agent 路由。 */
    private String sceneType;

    /** 会话标题，在创建新会话时使用。 */
    private String title;

    /** 用户发送的消息正文，不能为空。 */
    @NotBlank
    private String content;

    /** 消息类型，默认为 TEXT。 */
    private String messageType;

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

    public String getSceneType() {
        return sceneType;
    }

    public void setSceneType(String sceneType) {
        this.sceneType = sceneType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }
}
