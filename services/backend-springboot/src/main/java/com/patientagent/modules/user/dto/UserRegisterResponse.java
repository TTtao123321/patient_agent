package com.patientagent.modules.user.dto;

/**
 * 用户注册成功的响应 DTO。
 */
public class UserRegisterResponse {

    /** 新建用户的数据库主键 ID。 */
    private Long userId;
    /** 系统生成的用户业务编号。 */
    private String userNo;
    /** 账号创建时间字符串，由 JPA 监听器自动写入。 */
    private String createdAt;

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUserNo() {
        return userNo;
    }

    public void setUserNo(String userNo) {
        this.userNo = userNo;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
