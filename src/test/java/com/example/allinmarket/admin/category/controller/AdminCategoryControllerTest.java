package com.example.allinmarket.admin.category.controller;

import com.example.allinmarket.admin.category.service.AdminCategoryService;
import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.security.JwtProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.mockito.Mockito.when;

@WebMvcTest(AdminCategoryController.class)
@AutoConfigureRestTestClient
public class AdminCategoryControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AdminCategoryService adminCategoryService;

    // SecurityUtils가 (Long) authentication.getPrincipal()로 캐스팅하므로
    // principal을 반드시 Long 타입으로 설정해야 합니다.
    private void setAuthContext(Long userId) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(userId, null, List.of());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void 카테고리_전체_조회_성공_테스트() {
        // given
        setAuthContext(1L);

        List<CategoryDetailResponse> responses = List.of(
                new CategoryDetailResponse(1L, "전자제품", 1),
                new CategoryDetailResponse(2L, "패션", 2)
        );

        when(adminCategoryService.getCategories(1L)).thenReturn(responses);

        // when & then
        restTestClient.get().uri("/admin/categories")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.READ_SUCCESS.getMessage())
                .jsonPath("$.data[0].id").isEqualTo(1)
                .jsonPath("$.data[0].name").isEqualTo("전자제품")
                .jsonPath("$.data[0].sortOrder").isEqualTo(1)
                .jsonPath("$.data[1].id").isEqualTo(2)
                .jsonPath("$.data[1].name").isEqualTo("패션")
                .jsonPath("$.data[1].sortOrder").isEqualTo(2);
    }

    @Test
    void 카테고리_전체_조회_미인증_예외_테스트() {
        // given - SecurityContext 비어있는 상태 (인증 없음)
        SecurityContextHolder.clearContext();

        // when & then
        restTestClient.get().uri("/admin/categories")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void 카테고리_전체_조회_존재하지_않는_관리자_예외_테스트() {
        // given
        setAuthContext(999L);

        when(adminCategoryService.getCategories(999L))
                .thenThrow(new BaseException(ErrorEnum.ADMIN_NOT_FOUND));

        // when & then
        restTestClient.get().uri("/admin/categories")
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo(ErrorEnum.ADMIN_NOT_FOUND.getMessage());
    }
}
