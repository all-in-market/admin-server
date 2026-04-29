package com.example.allinmarket.admin.auth.service;

import com.example.allinmarket.admin.auth.dto.request.AdminLoginRequest;
import com.example.allinmarket.admin.auth.dto.response.LoginResult;
import com.example.allinmarket.admin.entity.Admin;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.auth.dto.response.BuyerLoginResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

        // Refresh 토큰 유효기간 일주일로 설정
        redisTemplate.opsForValue().set("refresh:" + refreshToken, admin.getId(), 7, TimeUnit.DAYS);

        BuyerLoginResponse response = new BuyerLoginResponse(accessToken);
        return new LoginResult(response, refreshToken);
    }
}
