package com.patientagent.modules.user.dto;

/**
 * 用户登录成功的响应 DTO。
 */
public class UserLoginResponse {

    /** 登录令牌，后续请求需在 Authorization 请求头中携带：{@code Bearer <accessToken>}。 */
    private String accessToken;
    /** Token 有效期（秒）。 */
    private Integer expiresIn;
    /** 登录用户的基本信息。 */
    private UserInfoResponse user;

    public String getAccessToken() {
        return accessToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public Integer getExpiresIn() {
        return expiresIn;
    }

    public void setExpiresIn(Integer expiresIn) {
        this.expiresIn = expiresIn;
    }

    public UserInfoResponse getUser() {
        return user;
    }

    public void setUser(UserInfoResponse user) {
        this.user = user;
    }
}
