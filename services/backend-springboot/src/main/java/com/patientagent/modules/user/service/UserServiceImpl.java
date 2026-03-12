package com.patientagent.modules.user.service;

import com.patientagent.modules.user.dto.UserInfoResponse;
import com.patientagent.modules.user.dto.UserLoginRequest;
import com.patientagent.modules.user.dto.UserLoginResponse;
import com.patientagent.modules.user.dto.UserRegisterRequest;
import com.patientagent.modules.user.dto.UserRegisterResponse;
import com.patientagent.modules.user.entity.UserEntity;
import com.patientagent.modules.user.repository.UserRepository;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@Service
public class UserServiceImpl implements UserService {

    private static final String TOKEN_PREFIX = "user:token:";
    private static final int TOKEN_EXPIRE_SECONDS = 7 * 24 * 60 * 60;

    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    public UserServiceImpl(UserRepository userRepository, StringRedisTemplate stringRedisTemplate) {
        this.userRepository = userRepository;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional
    public UserRegisterResponse register(UserRegisterRequest request) {
        if (userRepository.existsByUsernameAndIsDeleted(request.getUsername(), 0)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByPhoneAndIsDeleted(request.getPhone(), 0)) {
            throw new IllegalArgumentException("Phone already exists");
        }

        UserEntity entity = new UserEntity();
        entity.setUserNo(generateUserNo());
        entity.setUsername(request.getUsername());
        entity.setPasswordHash(hashPassword(request.getPassword()));
        entity.setRealName(request.getRealName());
        entity.setGender(request.getGender() == null ? 0 : request.getGender());
        entity.setPhone(request.getPhone());
        entity.setEmail(request.getEmail());

        if (request.getBirthDate() != null && !request.getBirthDate().isBlank()) {
            entity.setBirthDate(LocalDate.parse(request.getBirthDate()));
        }

        UserEntity saved = userRepository.save(entity);

        UserRegisterResponse response = new UserRegisterResponse();
        response.setUserId(saved.getId());
        response.setUserNo(saved.getUserNo());
        response.setCreatedAt(saved.getCreatedAt().toString());
        return response;
    }

    @Override
    @Transactional
    public UserLoginResponse login(UserLoginRequest request) {
        Optional<UserEntity> userOptional = userRepository.findByUsernameAndIsDeleted(request.getUsername(), 0);
        if (userOptional.isEmpty()) {
            throw new IllegalArgumentException("Invalid username or password");
        }

        UserEntity user = userOptional.get();
        if (!hashPassword(request.getPassword()).equals(user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid username or password");
        }
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new IllegalStateException("User is disabled");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        String token = UUID.randomUUID().toString().replace("-", "");
        stringRedisTemplate.opsForValue().set(TOKEN_PREFIX + token, String.valueOf(user.getId()), TOKEN_EXPIRE_SECONDS, TimeUnit.SECONDS);

        UserLoginResponse response = new UserLoginResponse();
        response.setAccessToken(token);
        response.setExpiresIn(TOKEN_EXPIRE_SECONDS);
        response.setUser(toUserInfo(user));
        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfoResponse getCurrentUser(String authorizationHeader) {
        String token = parseBearerToken(authorizationHeader);
        String userId = stringRedisTemplate.opsForValue().get(TOKEN_PREFIX + token);
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("Invalid or expired token");
        }

        UserEntity user = userRepository.findByIdAndIsDeleted(Long.parseLong(userId), 0)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toUserInfo(user);
    }

    private String parseBearerToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new IllegalArgumentException("Authorization header is required");
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authorization must use Bearer token");
        }
        String token = authorizationHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new IllegalArgumentException("Token is empty");
        }
        return token;
    }

    private UserInfoResponse toUserInfo(UserEntity user) {
        UserInfoResponse response = new UserInfoResponse();
        response.setUserId(user.getId());
        response.setUserNo(user.getUserNo());
        response.setUsername(user.getUsername());
        response.setRealName(user.getRealName());
        response.setGender(user.getGender());
        response.setBirthDate(user.getBirthDate() == null ? null : user.getBirthDate().toString());
        response.setPhone(user.getPhone());
        response.setEmail(user.getEmail());
        response.setStatus(user.getStatus());
        return response;
    }

    private String generateUserNo() {
        String ts = DateTimeFormatter.ofPattern("yyyyMMddHHmmss").format(LocalDateTime.now());
        int random = ThreadLocalRandom.current().nextInt(1000, 10000);
        return "U" + ts + random;
    }

    private String hashPassword(String rawPassword) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawPassword.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to hash password", ex);
        }
    }
}
