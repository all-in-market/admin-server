package com.example.allinmarket.admin.refund.controller;

import com.example.allinmarket.admin.refund.dto.request.DenyRefundRequest;
import com.example.allinmarket.admin.refund.dto.response.AuthorizeRefundResponse;
import com.example.allinmarket.admin.refund.facade.AdminRefundFacade;
import com.example.allinmarket.admin.refund.service.AdminRefundService;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(AdminRefundController.class)
@AutoConfigureRestTestClient
class AdminRefundControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AdminRefundService adminRefundService;

    @MockitoBean
    private AdminRefundFacade adminRefundFacade;

    @MockitoBean
    private RedisTemplate<String, Object> redisTemplate;

    private void setAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private AuthorizeRefundResponse createRefundResponse(Long refundId, RefundStatus status) {
        return new AuthorizeRefundResponse(
                refundId, 1L, 1L,
                ReasonEnum.CHANGE_OF_MIND,
                "환불 사유", null,
                status, null
        );
    }

    // ==================== 목록 조회 ====================

    @Test
    void 관리자_환불목록_조회_성공_테스트() {
        // given
        setAuth();

        PageResponse<AuthorizeRefundResponse> pageResponse = new PageResponse<>(
                List.of(createRefundResponse(1L, RefundStatus.PENDING)),
                1, 1, 1L, 10, true
        );

        when(adminRefundService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/admin/refunds")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.content[0].id").isEqualTo(1)
                .jsonPath("$.data.content[0].status").isEqualTo("PENDING")
                .jsonPath("$.data.totalElements").isEqualTo(1)
                .jsonPath("$.data.currentPage").isEqualTo(1);
    }

    @Test
    void 관리자_환불목록_조회_빈목록_성공_테스트() {
        // given
        setAuth();

        PageResponse<AuthorizeRefundResponse> emptyPage = new PageResponse<>(
                List.of(), 1, 0, 0L, 10, true
        );

        when(adminRefundService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(emptyPage);

        // when & then
        restTestClient.get().uri("/admin/refunds")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.content").isEmpty()
                .jsonPath("$.data.totalElements").isEqualTo(0);
    }

    @Test
    void 관리자_환불목록_조회_페이징_파라미터_성공_테스트() {
        // given
        setAuth();

        PageResponse<AuthorizeRefundResponse> pageResponse = new PageResponse<>(
                List.of(), 2, 5, 42L, 10, false
        );

        when(adminRefundService.findAll(any(Long.class), any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/admin/refunds?page=1&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.currentPage").isEqualTo(2)
                .jsonPath("$.data.totalPages").isEqualTo(5)
                .jsonPath("$.data.totalElements").isEqualTo(42)
                .jsonPath("$.data.isLast").isEqualTo(false);
    }

    // ==================== 환불 거절 ====================

    @Test
    void 관리자_환불거절_성공_테스트() {
        // given
        setAuth();

        when(adminRefundService.deny(any(Long.class), any(Long.class), any(DenyRefundRequest.class)))
                .thenReturn(createRefundResponse(1L, RefundStatus.DENIED));

        String requestBody = """
                {
                    "deniedReason": "환불 불가 사유"
                }
                """;

        // when & then
        restTestClient.put().uri("/admin/refunds/1/deny")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.status").isEqualTo("DENIED");
    }

    @Test
    void 관리자_환불거절_환불없음_실패_테스트() {
        // given
        setAuth();

        when(adminRefundService.deny(any(Long.class), any(Long.class), any(DenyRefundRequest.class)))
                .thenThrow(new BaseException(ErrorEnum.REFUND_NOT_FOUND));

        String requestBody = """
                {
                    "deniedReason": "환불 불가 사유"
                }
                """;

        // when & then
        restTestClient.put().uri("/admin/refunds/999/deny")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    // ==================== 환불 승인 ====================

    @Test
    void 관리자_환불승인_성공_테스트() {
        // given
        setAuth();

        when(adminRefundFacade.processRefund(any(Long.class), any(Long.class)))
                .thenReturn(createRefundResponse(1L, RefundStatus.SUCCESS));

        // when & then
        restTestClient.put().uri("/admin/refunds/1/complete")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.id").isEqualTo(1)
                .jsonPath("$.data.status").isEqualTo("SUCCESS");
    }

    @Test
    void 관리자_환불승인_환불없음_실패_테스트() {
        // given
        setAuth();

        when(adminRefundFacade.processRefund(any(Long.class), any(Long.class)))
                .thenThrow(new BaseException(ErrorEnum.REFUND_NOT_FOUND));

        // when & then
        restTestClient.put().uri("/admin/refunds/999/complete")
                .exchange()
                .expectStatus().is4xxClientError();
    }
}