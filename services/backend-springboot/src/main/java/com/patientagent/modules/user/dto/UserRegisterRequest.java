package com.patientagent.modules.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 用户注册请求 DTO。
 * <p>所有字段均附带参数校验注解，验证失败会由 {@code GlobalExceptionHandler} 统一处理。</p>
 */
public class UserRegisterRequest {

    /** 登录用户名，3-64 个字符。 */
    @NotBlank
    @Size(min = 3, max = 64)
    private String username;

    /** 登录密码（明文），6-64 个字符，服务层在存储前会做 SHA-256 哈希。 */
    @NotBlank
    @Size(min = 6, max = 64)
    private String password;

    /** 真实姓名，最多 64 个字符。 */
    @NotBlank
    @Size(max = 64)
    private String realName;

    /** 性别：0 = 未知，1 = 男，2 = 女；可为空。 */
    private Integer gender;

    /** 生日，格式：{@code yyyy-MM-dd}；可为空。 */
    private String birthDate;

    /** 手机号，6-20 个字符（支持 + 和 -）。 */
    @NotBlank
    @Pattern(regexp = "^[0-9+\\-]{6,20}$")
    private String phone;

    /** 电子邮箱，最多 128 个字符；可为空。 */
    @Size(max = 128)
    private String email;

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
}
