package com.example.allinmarket.admin.auth.service;

import com.example.allinmarket.admin.auth.dto.request.AdminLoginRequest;
import com.example.allinmarket.admin.auth.dto.response.AdminLoginResponse;
import com.example.allinmarket.admin.entity.Admin;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.auth.dto.response.BuyerLoginResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminAuthService {
    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtProvider jwtProvider;

    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.email()).orElseThrow(
                () -> new BaseException(ErrorEnum.ADMIN_NOT_FOUND)
        );

        if (admin.getDeletedAt() != null) {
            throw new BaseException(ErrorEnum.ADMIN_ALREADY_DELETED);
        }

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new BaseException(ErrorEnum.PASSWORD_MISMATCH);
        }

        String token = jwtProvider.generateToken(admin.getId(), admin.getRole());

        return new AdminLoginResponse(token);
    }
}
