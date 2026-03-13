package com.patientagent.modules.user.dto;

/**
 * 用户基本信息响应 DTO。
 * <p>登录响应、获取当前用户接口均复用此类返回用户信息。</p>
 */
public class UserInfoResponse {

    /** 数据库主键 ID。 */
    private Long userId;
    /** 系统生成的用户业务编号。 */
    private String userNo;
    /** 登录用户名。 */
    private String username;
    /** 真实姓名。 */
    private String realName;
    /** 性别：0 = 未知，1 = 男，2 = 女。 */
    private Integer gender;
    /** 生日，格式：{@code yyyy-MM-dd}。 */
    private String birthDate;
    /** 手机号。 */
    private String phone;
    /** 电子邮箱。 */
    private String email;
    /** 账号状态：1 = 正常，0 = 禁用。 */
    private Integer status;

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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public Integer getGender() {
        return gender;
    }

    public void setGender(Integer gender) {
        this.gender = gender;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Integer getStatus() {
        return status;
    }

    public void setStatus(Integer status) {
        this.status = status;
    }
}
