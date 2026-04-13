package com.example.allinmarket.admin.order.auth.service;


import com.example.allinmarket.admin.auth.dto.request.AdminLoginRequest;
import com.example.allinmarket.admin.auth.dto.response.AdminLoginResponse;
import com.example.allinmarket.admin.auth.service.AdminAuthService;
import com.example.allinmarket.admin.entity.Admin;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AdminAuthServiceTest {
    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminAuthService adminAuthService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtProvider jwtProvider;

    @Test
    void 로그인_성공_테스트() {
        // given
        AdminLoginRequest request = new AdminLoginRequest(
                "관리자@테스트.com",
                "12345678"
        );

        Admin admin = Admin.of(
                "관리자@테스트.com",
                "비밀번호암호화",
                "테스트"
        );

        ReflectionTestUtils.setField(admin, "id", 1L);
        ReflectionTestUtils.setField(admin, "role", UserRole.ADMIN);

        given(adminRepository.findByEmail("관리자@테스트.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("12345678", "비밀번호암호화")).willReturn(true);
        given(jwtProvider.generateToken(admin.getId(), admin.getRole())).willReturn("test-accessToken");

        // when
        AdminLoginResponse response = adminAuthService.login(request);

        // then
        assertThat(response.accessToken()).isEqualTo("test-accessToken");
    }

    @Test
    void 로그인_실패_테스트() {
        // given
        AdminLoginRequest request = new AdminLoginRequest(
                "관리자@테스트.com",
                "12345678"
        );

        given(adminRepository.findByEmail("관리자@테스트.com"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.ADMIN_NOT_FOUND.getMessage());
    }
}
