package com.example.allinmarket.admin.auth.controller;

import com.example.allinmarket.admin.auth.dto.request.AdminLoginRequest;
import com.example.allinmarket.admin.auth.service.AdminAuthService;
import com.example.allinmarket.common.auth.dto.LoginResponse;
import com.example.allinmarket.common.auth.dto.LoginResult;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.enums.UserRole;
import com.example.allinmarket.common.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@WebMvcTest(AdminAuthController.class)
@AutoConfigureRestTestClient
public class AdminAuthControllerTest {
    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AdminAuthService adminAuthService;

    @Test
    void 로그인_성공_테스트() {
        // given
        AdminLoginRequest request = new AdminLoginRequest(
                "관리자@테스트.com",
                "12345678"
        );

        LoginResult result = new LoginResult(new LoginResponse("test-accessToken"), "test-refreshToken");

        given(adminAuthService.login(any(AdminLoginRequest.class))).willReturn(result);

        // when & then
        restTestClient.post().uri("/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches(HttpHeaders.SET_COOKIE, ".*refreshToken=test-refreshToken.*")
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.LOGIN_SUCCESS.getMessage())
                .jsonPath("$.data.accessToken").isEqualTo("test-accessToken");
    }

    @Test
    void 로그인_실패_테스트() {
        // given
        AdminLoginRequest request = new AdminLoginRequest(
                "관리자@테스트.com",
                ""
        );

        // when & then
        restTestClient.post()
                .uri("/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").exists();
    }

    @Test
    void 토큰_갱신_성공_테스트() {
        // given
        LoginResult result = new LoginResult(new LoginResponse("new-accessToken"), "new-refreshToken");

        given(adminAuthService.refresh("test-refreshToken")).willReturn(result);

        // when & then
        restTestClient.post().uri("/admin/auth/refresh")
                .cookie("refreshToken", "test-refreshToken")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueMatches(HttpHeaders.SET_COOKIE, ".*refreshToken=new-refreshToken.*")
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(201)
                .jsonPath("$.message").isEqualTo(SuccessEnum.TOKEN_REFRESHED.getMessage())
                .jsonPath("$.data.accessToken").isEqualTo("new-accessToken");
    }

    @Test
    void 로그아웃_성공_테스트() {
        // JWT 필터가 Bearer 토큰을 검증하므로 유효한 토큰으로 설정
        given(jwtProvider.validateToken("test-accessToken")).willReturn(true);
        given(jwtProvider.getUserId("test-accessToken")).willReturn(1L);
        given(jwtProvider.getRole("test-accessToken")).willReturn(UserRole.ADMIN);

        // when & then
        restTestClient.post().uri("/admin/auth/logout")
                .header("Authorization", "Bearer test-accessToken")
                .cookie("refreshToken", "test-refreshToken")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.LOGOUT_SUCCESS.getMessage());
    }
}
