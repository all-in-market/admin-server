package com.example.allinmarket.admin.auth.service;

import com.example.allinmarket.admin.auth.dto.request.AdminLoginRequest;
import com.example.allinmarket.admin.entity.Admin;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.common.auth.dto.LoginResponse;
import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional
public class AdminAuthService {
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;
    private final RedisTemplate<String, Object> redisTemplate;

    public LoginResult login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.email()).orElseThrow(
                () -> {
                    log.warn("로그인 실패: {}", ErrorEnum.ADMIN_NOT_FOUND.getMessage());
                    return new BaseException(ErrorEnum.LOGIN_FAILED);
                }
        );

        if (admin.getDeletedAt() != null) {
            log.warn("로그인 실패: {}", ErrorEnum.BUYER_ALREADY_DELETED);
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            log.warn("로그인 실패: {}", ErrorEnum.PASSWORD_MISMATCH);
            throw new BaseException(ErrorEnum.LOGIN_FAILED);
        }

        String accessToken = jwtProvider.generateToken(admin.getId(), admin.getRole());
        String refreshToken = UUID.randomUUID().toString();

        redisTemplate.opsForValue().set("refresh:" + refreshToken, admin.getId(), 7, TimeUnit.DAYS);
        redisTemplate.opsForSet().add("refreshTokens:" + admin.getId(), refreshToken);
        redisTemplate.expire("refreshTokens:" + admin.getId(), 7, TimeUnit.DAYS);

        LoginResponse response = new LoginResponse(accessToken);
        return new LoginResult(response, refreshToken);
    }

    public LoginResult refresh(String refreshToken) {
        Long userId = (Long) redisTemplate.opsForValue().getAndDelete("refresh:" + refreshToken);
        if (userId == null) {
            throw new BaseException(ErrorEnum.TOKEN_EXPIRED);
        }

        Admin admin = adminRepository.findById(userId).orElseThrow(
                () -> new BaseException(ErrorEnum.ADMIN_NOT_FOUND)
        );
        if (admin.getDeletedAt() != null) {
            throw new BaseException(ErrorEnum.ADMIN_ALREADY_DELETED);
        }

        redisTemplate.opsForSet().remove("refreshTokens:" + userId, refreshToken);

        String newRefreshToken = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set("refresh:" + newRefreshToken, userId, 7, TimeUnit.DAYS);
        redisTemplate.opsForSet().add("refreshTokens:" + userId, newRefreshToken);
        redisTemplate.expire("refreshTokens:" + userId, 7, TimeUnit.DAYS);

        String newAccessToken = jwtProvider.generateToken(userId, admin.getRole());
        return new LoginResult(new LoginResponse(newAccessToken), newRefreshToken);
    }

    public void logout(String accessToken) {
        Long userId = jwtProvider.getUserId(accessToken);
        Set<Object> tokens = redisTemplate.opsForSet().members("refreshTokens:" + userId);
        if (tokens != null) {
            tokens.forEach(token -> redisTemplate.delete("refresh:" + token));
            redisTemplate.delete("refreshTokens:" + userId);
        }
        long remaining = jwtProvider.getRemainingExpiration(accessToken);
        if (remaining > 0) {
            redisTemplate.opsForValue()
                    .set("blacklist:" + accessToken, "logout", remaining, TimeUnit.MILLISECONDS);
        }
    }
}
