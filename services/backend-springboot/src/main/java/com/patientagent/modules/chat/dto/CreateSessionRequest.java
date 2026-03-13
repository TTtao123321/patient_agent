package com.patientagent.modules.chat.dto;

import jakarta.validation.constraints.NotNull;

/**
 * 创建新会话请求（不发送第一条消息，仅初始化会话记录）。
 */
public class CreateSessionRequest {

    @NotNull
    private Long userId;

    private String title;

    /** 场景类型，默认 mixed。 */
    private String sceneType;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSceneType() { return sceneType; }
    public void setSceneType(String sceneType) { this.sceneType = sceneType; }
}
