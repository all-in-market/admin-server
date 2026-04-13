package com.example.allinmarket.admin.order.controller;

import com.example.allinmarket.admin.order.service.AdminOrderService;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.JwtProvider;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.client.RestTestClient;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@WebMvcTest(AdminOrderController.class)
@AutoConfigureRestTestClient
class AdminOrderControllerTest {

    @Autowired
    private RestTestClient restTestClient;

    @MockitoBean
    private JwtProvider jwtProvider;

    @MockitoBean
    private AdminOrderService adminOrderService;

    private void setAuth() {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(1L, null, List.of(new SimpleGrantedAuthority("ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    // ==================== 목록 조회 ====================

    @Test
    void 관리자_주문목록_조회_성공_테스트() {
        // given
        setAuth();

        OrderDetailResponse response = new OrderDetailResponse(
                1L, 2L, BigDecimal.valueOf(30000),
                OrderStatus.PAID, "TRACK123",
                "홍길동", "서울시 강남구"
        );

        PageResponse<OrderDetailResponse> pageResponse = new PageResponse<>(
                List.of(response), 1, 1, 1L, 10, true
        );

        when(adminOrderService.findAll(any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/admin/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.content[0].orderId").isEqualTo(1)
                .jsonPath("$.data.content[0].status").isEqualTo("PAID")
                .jsonPath("$.data.totalElements").isEqualTo(1)
                .jsonPath("$.data.currentPage").isEqualTo(1);
    }

    @Test
    void 관리자_주문목록_조회_빈목록_성공_테스트() {
        // given
        setAuth();

        PageResponse<OrderDetailResponse> emptyPage = new PageResponse<>(
                List.of(), 1, 0, 0L, 10, true
        );

        when(adminOrderService.findAll(any(Pageable.class)))
                .thenReturn(emptyPage);

        // when & then
        restTestClient.get().uri("/admin/orders")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.data.content").isEmpty()
                .jsonPath("$.data.totalElements").isEqualTo(0);
    }

    @Test
    void 관리자_주문목록_조회_페이징_파라미터_성공_테스트() {
        // given
        setAuth();

        PageResponse<OrderDetailResponse> pageResponse = new PageResponse<>(
                List.of(), 2, 5, 42L, 10, false
        );

        when(adminOrderService.findAll(any(Pageable.class)))
                .thenReturn(pageResponse);

        // when & then
        restTestClient.get().uri("/admin/orders?page=1&size=10")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.data.currentPage").isEqualTo(2)
                .jsonPath("$.data.totalPages").isEqualTo(5)
                .jsonPath("$.data.totalElements").isEqualTo(42)
                .jsonPath("$.data.isLast").isEqualTo(false);
    }

    // ==================== 상태 변경 ====================

    @Test
    void 관리자_주문상태_변경_성공_테스트() {
        // given
        setAuth();

        OrderDetailResponse response = new OrderDetailResponse(
                1L, 2L, BigDecimal.valueOf(30000),
                OrderStatus.SHIPPED, "TRACK123",
                "홍길동", "서울시 강남구"
        );

        when(adminOrderService.updateStaus(any(Long.class), any()))
                .thenReturn(response);

        String requestBody = """
                {
                    "status": "SHIPPED"
                }
                """;

        // when & then
        restTestClient.put().uri("/admin/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.status").isEqualTo(200)
                .jsonPath("$.data.orderId").isEqualTo(1)
                .jsonPath("$.data.status").isEqualTo("SHIPPED");
    }

    @Test
    void 관리자_주문상태_변경_주문없음_실패_테스트() {
        // given
        setAuth();

        when(adminOrderService.updateStaus(any(Long.class), any()))
                .thenThrow(new BaseException(ErrorEnum.ORDER_NOT_FOUND));

        String requestBody = """
                {
                    "status": "SHIPPED"
                }
                """;

        // when & then
        restTestClient.put().uri("/admin/orders/999/status")
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .exchange()
                .expectStatus().is4xxClientError();
    }

    @Test
    void 관리자_주문상태_변경_유효성검사_실패_테스트() {
        // given
        setAuth();

        String invalidRequestBody = """
                {
                    "status": "INVALID_STATUS"
                }
                """;

        // when & then
        restTestClient.put().uri("/admin/orders/1/status")
                .contentType(MediaType.APPLICATION_JSON)
                .body(invalidRequestBody)
                .exchange()
                .expectStatus().is4xxClientError();
    }
}