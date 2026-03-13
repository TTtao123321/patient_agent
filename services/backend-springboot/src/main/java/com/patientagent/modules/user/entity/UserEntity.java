package com.patientagent.modules.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 用户数据库实体，对应 {@code patient_user} 表。
 * <p>
 * 密码以 SHA-256 哈希存储，登录标识符由验证后存入 Redis。
 * 使用软删除（{@code is_deleted}），用户根据历史记录不会物理删除。
 * </p>
 */
@Entity
@Table(name = "patient_user")
public class UserEntity {

    /** 主键，数据库自增。 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 用户业务编号，全局唯一，格式：{@code U + yyyyMMddHHmmss + 4位随机数}。 */
    @Column(name = "user_no", nullable = false, length = 32, unique = true)
    private String userNo;

    /** 登录用户名，全局唯一。 */
    @Column(name = "username", nullable = false, length = 64, unique = true)
    private String username;

    /** SHA-256 哈希后的密码，不存储明文。 */
    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    /** 真实姓名。 */
    @Column(name = "real_name", nullable = false, length = 64)
    private String realName;

    /** 性别：0 = 未知，1 = 男，2 = 女。 */
    @Column(name = "gender", nullable = false)
    private Integer gender = 0;

    /** 生日（可为空）。 */
    @Column(name = "birth_date")
    private LocalDate birthDate;

    /** 手机号，全局唯一。 */
    @Column(name = "phone", nullable = false, length = 20, unique = true)
    private String phone;

    /** 电子邮箱（可为空）。 */
    @Column(name = "email", length = 128)
    private String email;

    /** 账号状态：1 = 正常，0 = 禁用。 */
    @Column(name = "status", nullable = false)
    private Integer status = 1;

    /** 最近登录时间（可为空）。 */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    /** 记录创建时间。 */
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    /** 记录最后更新时间。 */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 软删除标志：0 = 正常，1 = 已删除。 */
    @Column(name = "is_deleted", nullable = false)
    private Integer isDeleted = 0;

    /** JPA 持久化前调用，初始化默认字段。 */
    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.status == null) {
            this.status = 1;
        }
        if (this.gender == null) {
            this.gender = 0;
        }
        if (this.isDeleted == null) {
            this.isDeleted = 0;
        }
    }

    /** JPA 更新前调用，自动刷新 {@code updatedAt}。 */
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

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
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

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
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

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
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
