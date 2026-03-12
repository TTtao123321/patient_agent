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

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<UserRegisterResponse> register(@Valid @RequestBody UserRegisterRequest request) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, userService.register(request));
    }

    @PostMapping("/login")
    public ApiResponse<UserLoginResponse> login(@Valid @RequestBody UserLoginRequest request) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, userService.login(request));
    }

    @GetMapping("/me")
    public ApiResponse<UserInfoResponse> me(@RequestHeader("Authorization") String authorization) {
        String traceId = UUID.randomUUID().toString().replace("-", "");
        return ApiResponse.success(traceId, userService.getCurrentUser(authorization));
    }
}
