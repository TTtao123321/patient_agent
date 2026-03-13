package com.patientagent.modules.user.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 用户登录请求 DTO。
 */
public class UserLoginRequest {

    /** 登录用户名，不能为空。 */
    @NotBlank
    private String username;

    /** 登录密码（明文），服务层会对其 SHA-256 哈希后与库中密文比对。 */
    @NotBlank
    private String password;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
