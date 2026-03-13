package com.patientagent.modules.user.service;

import com.patientagent.modules.user.dto.UserInfoResponse;
import com.patientagent.modules.user.dto.UserLoginRequest;
import com.patientagent.modules.user.dto.UserLoginResponse;
import com.patientagent.modules.user.dto.UserRegisterRequest;
import com.patientagent.modules.user.dto.UserRegisterResponse;

/**
 * 用户管理服务接口，提供注册、登录和当前用户查询三个核心功能。
 */
public interface UserService {

    /**
     * 用户注册。
     * 校验用户名和手机号唯一性，对密码做 SHA-256 哈希后持久化到数据库。
     *
     * @param request 注册请求（用户名、密码、真实姓名、手机号等）
     * @return 包含新用户 ID 和用户编号的响应
     */
    UserRegisterResponse register(UserRegisterRequest request);

    /**
     * 用户登录。
     * 验证用户名密码后，在 Redis 中存储 token → userId 的映射并返回 accessToken。
     *
     * @param request 登录请求（用户名 + 密码）
     * @return 包含 accessToken、过期时间和用户信息的响应
     */
    UserLoginResponse login(UserLoginRequest request);

    /**
     * 获取当前登录用户信息。
     * 解析 HTTP 请求头中的 Bearer Token，从 Redis 查找对应用户 ID，再查库返回用户信息。
     *
     * @param authorizationHeader HTTP Authorization 请求头，格式：{@code Bearer <token>}
     * @return 当前用户的基本信息
     */
    UserInfoResponse getCurrentUser(String authorizationHeader);
}
