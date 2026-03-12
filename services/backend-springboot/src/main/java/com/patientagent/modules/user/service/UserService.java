package com.patientagent.modules.user.service;

import com.patientagent.modules.user.dto.UserInfoResponse;
import com.patientagent.modules.user.dto.UserLoginRequest;
import com.patientagent.modules.user.dto.UserLoginResponse;
import com.patientagent.modules.user.dto.UserRegisterRequest;
import com.patientagent.modules.user.dto.UserRegisterResponse;

public interface UserService {

    UserRegisterResponse register(UserRegisterRequest request);

    UserLoginResponse login(UserLoginRequest request);

    UserInfoResponse getCurrentUser(String authorizationHeader);
}
