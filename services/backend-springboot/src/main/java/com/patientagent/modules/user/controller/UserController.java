package com.patientagent.modules.user.controller;

import com.patientagent.common.response.ApiResponse;
import com.patientagent.modules.user.dto.UserInfoResponse;
import com.patientagent.modules.user.dto.UserLoginRequest;
import com.patientagent.modules.user.dto.UserLoginResponse;
import com.patientagent.modules.user.dto.UserRegisterRequest;
import com.patientagent.modules.user.dto.UserRegisterResponse;
import com.patientagent.modules.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * 用户模块 HTTP 入口，提供注册、登录和获取当前用户信息三个接口。
 * <ul>
 *   <li>{@code POST /api/v1/users/register} — 用户注册。</li>
 *   <li>{@code POST /api/v1/users/login}    — 用户登录，返回 accessToken。</li>
 *   <li>{@code GET  /api/v1/users/me}       — 基于 Bearer Token 获取当前用户信息。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 用户注册接口。
     */
    @PostMapping("/register")
    public ApiResponse<UserRegisterResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, userService.register(request));
    }

    /**
     * 用户登录接口，验证成功后返回 accessToken。
     */
    @PostMapping("/login")
    public ApiResponse<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, userService.login(request));
    }

    /**
     * 获取当前登录用户信息。
     * 需要在 Authorization 请求头中携带 Bearer Token。
     *
     * @param authorization 格式：{@code Bearer <token>}
     */
    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(@RequestHeader("Authorization") String authorization) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, userService.getCurrentUser(authorization));
    }
}
