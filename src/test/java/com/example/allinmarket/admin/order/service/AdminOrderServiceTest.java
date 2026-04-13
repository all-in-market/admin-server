package com.example.allinmarket.admin.order.service;

import com.example.allinmarket.admin.entity.Admin;
import com.example.allinmarket.admin.order.dto.request.AdminOrderUpdateStatusRequest;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
class AdminOrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AdminRepository adminRepository;

    @InjectMocks
    private AdminOrderService adminOrderService;

    private Order createOrderMock(Long orderId) {
        Buyer buyer = mock(Buyer.class);
        given(buyer.getId()).willReturn(1L);

        Order order = mock(Order.class);
        given(order.getId()).willReturn(orderId);
        given(order.getBuyer()).willReturn(buyer);
        given(order.getTotalAmount()).willReturn(BigDecimal.valueOf(30000));
        given(order.getStatus()).willReturn(OrderStatus.PAID);
        given(order.getTrackingNumber()).willReturn("TRACK123");
        given(order.getRecipient()).willReturn("홍길동");
        given(order.getAddress()).willReturn("서울시 강남구");
        return order;
    }

    // ==================== 목록 조회 ====================

    @Test
    void 관리자_주문목록_조회_성공_테스트() {
        // given
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Order order = createOrderMock(1L);
        Page<Order> page = new PageImpl<>(List.of(order), pageable, 1);

        given(orderRepository.findAllBy(pageable)).willReturn(page);

        // when
        PageResponse<OrderDetailResponse> result = adminOrderService.findAll(adminId, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(1L, result.content().get(0).orderId());
    }

    @Test
    void 관리자_주문목록_조회_관리자없음_실패_테스트() {
        // given
        Long adminId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(false);

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> adminOrderService.findAll(adminId, pageable)
        );
        assertEquals(ErrorEnum.ADMIN_NOT_FOUND, exception.getErrorEnum());
    }

    @Test
    void 관리자_주문목록_조회_빈목록_성공_테스트() {
        // given
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Page<Order> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        given(orderRepository.findAllBy(pageable)).willReturn(emptyPage);

        // when
        PageResponse<OrderDetailResponse> result = adminOrderService.findAll(adminId, pageable);

        // then
        assertNotNull(result);
        assertEquals(0, result.totalElements());
        assertTrue(result.content().isEmpty());
        assertTrue(result.isLast());
    }

    @Test
    void 관리자_주문목록_조회_페이징_테스트() {
        // given
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(0, 2);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Order order1 = createOrderMock(1L);
        Order order2 = createOrderMock(2L);
        Page<Order> page = new PageImpl<>(List.of(order1, order2), pageable, 3);

        given(orderRepository.findAllBy(pageable)).willReturn(page);

        // when
        PageResponse<OrderDetailResponse> result = adminOrderService.findAll(adminId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(2, result.content().size());
        assertEquals(2, result.totalPages());
        assertFalse(result.isLast());
    }

    @Test
    void 관리자_주문목록_조회_두번째_페이지_테스트() {
        // given
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(1, 2);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Order order3 = createOrderMock(3L);
        Page<Order> page = new PageImpl<>(List.of(order3), pageable, 3);

        given(orderRepository.findAllBy(pageable)).willReturn(page);

        // when
        PageResponse<OrderDetailResponse> result = adminOrderService.findAll(adminId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(2, result.currentPage());
        assertTrue(result.isLast());
    }

    // ==================== 상태 변경 ====================

    @Test
    void 관리자_주문상태_변경_성공_테스트() {
        // given
        Long adminId = 1L;
        Long orderId = 1L;

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Order order = createOrderMock(orderId);
        AdminOrderUpdateStatusRequest request = new AdminOrderUpdateStatusRequest(OrderStatus.SHIPPED);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // when
        OrderDetailResponse result = adminOrderService.updateStaus(adminId, orderId, request);

        // then
        assertNotNull(result);
        assertEquals(orderId, result.orderId());
    }

    @Test
    void 관리자_주문상태_변경_관리자없음_실패_테스트() {
        // given
        Long adminId = 999L;
        Long orderId = 1L;

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(false);

        AdminOrderUpdateStatusRequest request = new AdminOrderUpdateStatusRequest(OrderStatus.SHIPPED);

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> adminOrderService.updateStaus(adminId, orderId, request)
        );
        assertEquals(ErrorEnum.ADMIN_NOT_FOUND, exception.getErrorEnum());
    }

    @Test
    void 관리자_주문상태_변경_주문없음_실패_테스트() {
        // given
        Long adminId = 1L;
        Long orderId = 999L;

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        AdminOrderUpdateStatusRequest request = new AdminOrderUpdateStatusRequest(OrderStatus.SHIPPED);

        given(orderRepository.findById(orderId)).willReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> adminOrderService.updateStaus(adminId, orderId, request)
        );
        assertEquals(ErrorEnum.ORDER_NOT_FOUND, exception.getErrorEnum());
    }

    @Test
    void 관리자_주문상태_변경_불가상태_실패_테스트() {
        // given
        Long adminId = 1L;
        Long orderId = 1L;

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Order order = mock(Order.class);
        Buyer buyer = mock(Buyer.class);
        given(buyer.getId()).willReturn(1L);
        given(order.getId()).willReturn(orderId);
        given(order.getBuyer()).willReturn(buyer);
        given(order.getTotalAmount()).willReturn(BigDecimal.valueOf(30000));
        given(order.getTrackingNumber()).willReturn("TRACK123");
        given(order.getRecipient()).willReturn("홍길동");
        given(order.getAddress()).willReturn("서울시 강남구");

        AdminOrderUpdateStatusRequest request = new AdminOrderUpdateStatusRequest(OrderStatus.PAID);

        given(orderRepository.findById(orderId)).willReturn(Optional.of(order));

        // updateStatus 호출 시 예외 발생하도록 설정
        org.mockito.Mockito.doThrow(new BaseException(ErrorEnum.ORDER_STATUS_IMMUTABLE))
                .when(order).updateStatus(OrderStatus.PAID);

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> adminOrderService.updateStaus(adminId, orderId, request)
        );
        assertEquals(ErrorEnum.ORDER_STATUS_IMMUTABLE, exception.getErrorEnum());
    }
}