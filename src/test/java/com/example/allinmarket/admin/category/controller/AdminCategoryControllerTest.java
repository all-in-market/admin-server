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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verifyNoInteractions;
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

    // ==================== update ====================

    @Test
    void 카테고리_수정_성공_테스트() {
        // given
        setAuthContext(1L);

        CategoryDetailResponse response = new CategoryDetailResponse(10L, "가전제품", 2);

        when(adminCategoryService.update(eq(1L), eq(10L), any())).thenReturn(response);

        // when & then
        restTestClient.put().uri("/admin/categories/10")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "name": "가전제품",
                            "sortOrder": 2
                        }
                        """)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.message").isEqualTo(SuccessEnum.UPDATE_SUCCESS.getMessage())
                .jsonPath("$.data.id").isEqualTo(10)
                .jsonPath("$.data.name").isEqualTo("가전제품")
                .jsonPath("$.data.sortOrder").isEqualTo(2);
    }

    @Test
    void 카테고리_수정_미인증_예외_테스트() {
        // given - SecurityContext 비어있는 상태 (인증 없음)
        SecurityContextHolder.clearContext();

        // when & then
        restTestClient.put().uri("/admin/categories/10")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "name": "가전제품",
                            "sortOrder": 2
                        }
                        """)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(401);
    }

    @Test
    void 카테고리_수정_존재하지_않는_관리자_예외_테스트() {
        // given
        setAuthContext(999L);

        when(adminCategoryService.update(eq(999L), eq(10L), any()))
                .thenThrow(new BaseException(ErrorEnum.ADMIN_NOT_FOUND));

        // when & then
        restTestClient.put().uri("/admin/categories/10")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "name": "가전제품",
                            "sortOrder": 2
                        }
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo(ErrorEnum.ADMIN_NOT_FOUND.getMessage());
    }

    @Test
    void 카테고리_수정_존재하지_않는_카테고리_예외_테스트() {
        // given
        setAuthContext(1L);

        when(adminCategoryService.update(eq(1L), eq(999L), any()))
                .thenThrow(new BaseException(ErrorEnum.CATEGORY_NOT_FOUND));

        // when & then
        restTestClient.put().uri("/admin/categories/999")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "name": "가전제품",
                            "sortOrder": 2
                        }
                        """)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.message").isEqualTo(ErrorEnum.CATEGORY_NOT_FOUND.getMessage());
    }

    @Test
    void 카테고리_수정_이름_길이_초과_예외_테스트() {
        // given
        setAuthContext(1L);
        String tooLongName = "a".repeat(51);

        // when & then
        restTestClient.put().uri("/admin/categories/10")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "name": "%s",
                            "sortOrder": 2
                        }
                        """.formatted(tooLongName))
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("카테고리 이름은 50자를 초과할 수 없습니다.");

        verifyNoInteractions(adminCategoryService);
    }

    @Test
    void 카테고리_수정_sortOrder_음수_예외_테스트() {
        // given
        setAuthContext(1L);

        // when & then
        restTestClient.put().uri("/admin/categories/10")
                .contentType(MediaType.APPLICATION_JSON)
                .body("""
                        {
                            "name": "가전제품",
                            "sortOrder": -1
                        }
                        """)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.success").isEqualTo(false)
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.message").isEqualTo("순서는 0 또는 양수로 지정해 주세요");

        verifyNoInteractions(adminCategoryService);
    }
}
