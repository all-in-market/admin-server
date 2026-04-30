package com.example.allinmarket.admin.auth.service;


import com.example.allinmarket.admin.auth.dto.request.AdminLoginRequest;
import com.example.allinmarket.admin.entity.Admin;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

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

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOps;

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
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        // when
        LoginResult result = adminAuthService.login(request);

        // then
        assertThat(result.response().accessToken()).isEqualTo("test-accessToken");
        assertThat(result.refreshToken()).isNotNull();
    }

    @Test
    void 로그인_실패_어드민없음_테스트() {
        // given
        AdminLoginRequest request = new AdminLoginRequest(
                "관리자@테스트.com",
                "12345678"
        );

        given(adminRepository.findByEmail("관리자@테스트.com")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.LOGIN_FAILED.getMessage());
    }

    @Test
    void 로그인_실패_탈퇴한_어드민_테스트() {
        // given
        AdminLoginRequest request = new AdminLoginRequest(
                "관리자@테스트.com",
                "12345678"
        );

        Admin admin = Admin.of("관리자@테스트.com", "비밀번호암호화", "테스트");
        admin.delete();

        given(adminRepository.findByEmail("관리자@테스트.com")).willReturn(Optional.of(admin));

        // when & then
        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.LOGIN_FAILED.getMessage());
    }

    @Test
    void 로그인_실패_비밀번호_불일치_테스트() {
        // given
        AdminLoginRequest request = new AdminLoginRequest(
                "관리자@테스트.com",
                "wrongPwd1"
        );

        Admin admin = Admin.of("관리자@테스트.com", "비밀번호암호화", "테스트");

        given(adminRepository.findByEmail("관리자@테스트.com")).willReturn(Optional.of(admin));
        given(passwordEncoder.matches("wrongPwd1", "비밀번호암호화")).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminAuthService.login(request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.LOGIN_FAILED.getMessage());
    }

    @Test
    void 토큰_갱신_성공_테스트() {
        // given
        String oldRefreshToken = "old-refresh-token";
        Long userId = 1L;

        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("refresh:" + oldRefreshToken)).willReturn(userId);
        given(jwtProvider.generateToken(userId, UserRole.ADMIN)).willReturn("new-accessToken");

        // when
        LoginResult result = adminAuthService.refresh(oldRefreshToken);

        // then
        assertThat(result.response().accessToken()).isEqualTo("new-accessToken");
        assertThat(result.refreshToken()).isNotNull();
    }

    @Test
    void 토큰_갱신_실패_만료된_토큰_테스트() {
        // given
        String expiredToken = "expired-refresh-token";

        given(redisTemplate.opsForValue()).willReturn(valueOps);
        given(valueOps.get("refresh:" + expiredToken)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> adminAuthService.refresh(expiredToken))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.TOKEN_EXPIRED.getMessage());
    }

    @Test
    void 로그아웃_성공_테스트() {
        // given
        String accessToken = "test-access-token";
        String refreshToken = "test-refresh-token";
        long remaining = 5000L;

        given(jwtProvider.getRemainingExpiration(accessToken)).willReturn(remaining);
        given(redisTemplate.opsForValue()).willReturn(valueOps);

        // when
        adminAuthService.logout(accessToken, refreshToken);

        // then
        verify(valueOps).set("blacklist:" + accessToken, "logout", remaining, TimeUnit.MILLISECONDS);
        verify(redisTemplate).delete("refresh:" + refreshToken);
    }
}
