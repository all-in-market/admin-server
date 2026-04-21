package com.example.allinmarket.admin.refund.service;

import com.example.allinmarket.admin.refund.dto.request.DenyRefundRequest;
import com.example.allinmarket.admin.refund.dto.response.AuthorizeRefundResponse;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.entity.Buyer;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.entity.Order;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import com.example.allinmarket.domain.refund.repository.RefundRepository;
import com.example.allinmarket.domain.sellerdashboard.service.DashboardService;
import com.example.allinmarket.domain.transactionhistory.service.TransactionHistoryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AdminRefundServiceTest {

    @Mock
    private RefundRepository refundRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private TransactionHistoryService transactionHistoryService;

    @Mock
    private DashboardService dashboardService;

    @InjectMocks
    private AdminRefundService adminRefundService;

    private Refund createRefundMock(Long refundId) {
        Buyer buyer = mock(Buyer.class);
        given(buyer.getId()).willReturn(1L);

        Payment payment = mock(Payment.class);
        given(payment.getId()).willReturn(1L);

        Refund refund = mock(Refund.class);
        given(refund.getId()).willReturn(refundId);
        given(refund.getBuyer()).willReturn(buyer);
        given(refund.getPayment()).willReturn(payment);
        given(refund.getReason()).willReturn(ReasonEnum.CHANGE_OF_MIND);
        given(refund.getDescription()).willReturn("환불 사유");
        given(refund.getDeniedReason()).willReturn(null);
        given(refund.getStatus()).willReturn(RefundStatus.PENDING);
        given(refund.getProcessedAt()).willReturn(null);
        return refund;
    }

    // ==================== 목록 조회 ====================

    @Test
    void 관리자_환불목록_조회_성공_테스트() {
        // given
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Refund refund = createRefundMock(1L);
        Page<Refund> page = new PageImpl<>(List.of(refund), pageable, 1);

        given(refundRepository.findAllBy(pageable)).willReturn(page);

        // when
        PageResponse<AuthorizeRefundResponse> result = adminRefundService.findAll(adminId, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.totalElements());
        assertEquals(1, result.content().size());
        assertEquals(1L, result.content().get(0).id());
        assertEquals(RefundStatus.PENDING, result.content().get(0).status());
    }

    @Test
    void 관리자_환불목록_조회_관리자없음_실패_테스트() {
        // given
        Long adminId = 999L;
        Pageable pageable = PageRequest.of(0, 10);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(false);

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> adminRefundService.findAll(adminId, pageable)
        );
        assertEquals(ErrorEnum.ADMIN_NOT_FOUND, exception.getErrorEnum());
    }

    @Test
    void 관리자_환불목록_조회_빈목록_성공_테스트() {
        // given
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(0, 10);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Page<Refund> emptyPage = new PageImpl<>(List.of(), pageable, 0);
        given(refundRepository.findAllBy(pageable)).willReturn(emptyPage);

        // when
        PageResponse<AuthorizeRefundResponse> result = adminRefundService.findAll(adminId, pageable);

        // then
        assertNotNull(result);
        assertEquals(0, result.totalElements());
        assertTrue(result.content().isEmpty());
        assertTrue(result.isLast());
    }

    @Test
    void 관리자_환불목록_조회_페이징_테스트() {
        // given
        Long adminId = 1L;
        Pageable pageable = PageRequest.of(0, 2);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Refund refund1 = createRefundMock(1L);
        Refund refund2 = createRefundMock(2L);
        Page<Refund> page = new PageImpl<>(List.of(refund1, refund2), pageable, 3);

        given(refundRepository.findAllBy(pageable)).willReturn(page);

        // when
        PageResponse<AuthorizeRefundResponse> result = adminRefundService.findAll(adminId, pageable);

        // then
        assertEquals(3, result.totalElements());
        assertEquals(2, result.content().size());
        assertEquals(2, result.totalPages());
        assertFalse(result.isLast());
    }

    // ==================== 환불 거절 ====================

    @Test
    void 관리자_환불거절_성공_테스트() {
        // given
        Long adminId = 1L;
        Long refundId = 1L;

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Refund refund = createRefundMock(refundId);
        given(refundRepository.findByIdAndStatus(refundId, RefundStatus.PENDING))
                .willReturn(Optional.of(refund));

        DenyRefundRequest request = new DenyRefundRequest("환불 불가 사유");

        // when
        AuthorizeRefundResponse result = adminRefundService.deny(adminId, refundId, request);

        // then
        assertNotNull(result);
        assertEquals(refundId, result.id());
        verify(refund).deny("환불 불가 사유");
    }

    @Test
    void 관리자_환불거절_관리자없음_실패_테스트() {
        // given
        Long adminId = 999L;
        Long refundId = 1L;

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(false);

        DenyRefundRequest request = new DenyRefundRequest("환불 불가 사유");

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> adminRefundService.deny(adminId, refundId, request)
        );
        assertEquals(ErrorEnum.ADMIN_NOT_FOUND, exception.getErrorEnum());
    }

    @Test
    void 관리자_환불거절_환불없음_실패_테스트() {
        // given
        Long adminId = 1L;
        Long refundId = 999L;

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);
        given(refundRepository.findByIdAndStatus(refundId, RefundStatus.PENDING))
                .willReturn(Optional.empty());

        DenyRefundRequest request = new DenyRefundRequest("환불 불가 사유");

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> adminRefundService.deny(adminId, refundId, request)
        );
        assertEquals(ErrorEnum.REFUND_NOT_FOUND, exception.getErrorEnum());
    }

    // ==================== 환불 승인 ====================

    @Test
    void 관리자_환불승인_성공_테스트() {
        // given
        Long adminId = 1L;
        Long refundId = 1L;

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);

        Order order = mock(Order.class);
        given(order.getId()).willReturn(1L);

        Payment payment = mock(Payment.class);
        given(payment.getId()).willReturn(1L);
        given(payment.getOrder()).willReturn(order);

        Buyer buyer = mock(Buyer.class);
        given(buyer.getId()).willReturn(1L);

        Refund refund = mock(Refund.class);
        given(refund.getId()).willReturn(refundId);
        given(refund.getBuyer()).willReturn(buyer);
        given(refund.getPayment()).willReturn(payment);
        given(refund.getReason()).willReturn(ReasonEnum.CHANGE_OF_MIND);
        given(refund.getDescription()).willReturn("환불 사유");
        given(refund.getDeniedReason()).willReturn(null);
        given(refund.getStatus()).willReturn(RefundStatus.PENDING);
        given(refund.getProcessedAt()).willReturn(null);

        given(refundRepository.findByIdAndStatus(refundId, RefundStatus.PENDING))
                .willReturn(Optional.of(refund));

        // when
        AuthorizeRefundResponse result = adminRefundService.complete(adminId, refundId);

        // then
        assertNotNull(result);
        assertEquals(refundId, result.id());
        verify(refund).complete();
        verify(payment).cancel();
        verify(order).updateStatus(OrderStatus.REFUNDED);
        verify(dashboardService).updateSellerDashboardWithRefund(1L);
    }

    @Test
    void 관리자_환불승인_관리자없음_실패_테스트() {
        // given
        Long adminId = 999L;
        Long refundId = 1L;

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(false);

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> adminRefundService.complete(adminId, refundId)
        );
        assertEquals(ErrorEnum.ADMIN_NOT_FOUND, exception.getErrorEnum());
    }

    @Test
    void 관리자_환불승인_환불없음_실패_테스트() {
        // given
        Long adminId = 1L;
        Long refundId = 999L;

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);
        given(refundRepository.findByIdAndStatus(refundId, RefundStatus.PENDING))
                .willReturn(Optional.empty());

        // when & then
        BaseException exception = assertThrows(
                BaseException.class,
                () -> adminRefundService.complete(adminId, refundId)
        );
        assertEquals(ErrorEnum.REFUND_NOT_FOUND, exception.getErrorEnum());
    }
}