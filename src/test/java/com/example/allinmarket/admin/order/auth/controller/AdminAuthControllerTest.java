package com.example.allinmarket.admin.order.auth.controller;

import com.example.allinmarket.admin.auth.controller.AdminAuthController;
import com.example.allinmarket.admin.auth.dto.request.AdminLoginRequest;
import com.example.allinmarket.admin.auth.dto.response.AdminLoginResponse;
import com.example.allinmarket.admin.auth.service.AdminAuthService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.security.JwtProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
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

        AdminLoginResponse response = new AdminLoginResponse("test-accessToken");

        given(adminAuthService.login(any(AdminLoginRequest.class))).willReturn(response);

        // when & then
        restTestClient.post().uri("/admin/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.LOGIN_SUCCESS.getMessage());
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
}
