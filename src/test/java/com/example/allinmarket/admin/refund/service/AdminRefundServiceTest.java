package com.example.allinmarket.admin.refund.service;

import com.example.allinmarket.admin.refund.client.PortOnePaymentResponse;
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
import com.example.allinmarket.domain.payment.enums.MethodEnum;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    private static final Long REFUND_ID = 1L;
    private static final Long PAYMENT_ID = 10L;
    private static final Long ORDER_ID = 100L;
    private static final String IMP_UID = "imp_mock_123";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);

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

    // ==================== 환불 처리 시작 ====================

    @Test
    void startRefundProcessing_PENDING_환불이면_PROCESSING으로_변경된다() {
        // given
        Refund refund = createPendingRefund();

        given(refundRepository.findByIdWithPayment(REFUND_ID))
                .willReturn(Optional.of(refund));

        // when
        Refund result = adminRefundService.startRefundProcessing(REFUND_ID);

        // then
        assertThat(result.getStatus()).isEqualTo(RefundStatus.PROCESSING);
    }

    @Test
    void startRefundProcessing_PENDING이_아니면_예외가_발생한다() {
        // given
        Refund refund = createPendingRefund();
        refund.deny("거절 사유");

        given(refundRepository.findByIdWithPayment(REFUND_ID))
                .willReturn(Optional.of(refund));

        // when & then
        assertThatThrownBy(() -> adminRefundService.startRefundProcessing(REFUND_ID))
                .isInstanceOf(BaseException.class);
    }

    @Test
    void failProcessing_PROCESSING_환불이면_FAILED로_변경된다() {
        // given
        Refund refund = createProcessingRefund();

        given(refundRepository.findById(REFUND_ID))
                .willReturn(Optional.of(refund));

        // when
        adminRefundService.failProcessing(REFUND_ID);

        // then
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);
    }

    // ==================== 환불 승인 complete ====================

    @Test
    void complete_취소된_PG응답이면_환불성공_결제환불_주문환불로_변경된다() {
        // given
        Refund refund = createProcessingRefund();

        when(refundRepository.findByIdWithPaymentAndOrder(REFUND_ID))
                .thenReturn(Optional.of(refund));

        PortOnePaymentResponse canceledPayment = cancelledPaymentResponse(IMP_UID);

        // when
        AuthorizeRefundResponse response =
                adminRefundService.complete(REFUND_ID, canceledPayment);

        // then
        assertThat(response.status()).isEqualTo(RefundStatus.SUCCESS);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCESS);
        assertThat(refund.getProcessedAt()).isNotNull();

        Payment payment = refund.getPayment();
        assertThat(payment.getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(payment.getOrder().getStatus()).isEqualTo(OrderStatus.REFUNDED);

        verify(transactionHistoryService).saveRefundHistory(refund);
        verify(transactionHistoryService).savePaymentHistory(payment);
        verify(dashboardService).updateSellerDashboardWithRefund(ORDER_ID);
    }

    @Test
    void complete_PG응답이_CANCELLED가_아니면_환불실패로_변경된다() {
        // given
        Refund refund = createProcessingRefund();

        when(refundRepository.findByIdWithPaymentAndOrder(REFUND_ID))
                .thenReturn(Optional.of(refund));

        PortOnePaymentResponse paidPayment = paidPaymentResponse(IMP_UID);

        // when
        AuthorizeRefundResponse response =
                adminRefundService.complete(REFUND_ID, paidPayment);

        // then
        assertThat(response.status()).isEqualTo(RefundStatus.FAILED);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.FAILED);

        assertThat(refund.getPayment().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(refund.getPayment().getOrder().getStatus()).isEqualTo(OrderStatus.PAID);

        verifyNoInteractions(transactionHistoryService);
        verifyNoInteractions(dashboardService);
    }

    @Test
    void complete_PG응답의_impUid가_DB와_다르면_예외가_발생한다() {
        // given
        Refund refund = createProcessingRefund();

        when(refundRepository.findByIdWithPaymentAndOrder(REFUND_ID))
                .thenReturn(Optional.of(refund));

        PortOnePaymentResponse wrongPayment = cancelledPaymentResponse("wrong_imp_uid");

        // when & then
        assertThatThrownBy(() -> adminRefundService.complete(REFUND_ID, wrongPayment))
                .isInstanceOf(BaseException.class);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
        assertThat(refund.getPayment().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(refund.getPayment().getOrder().getStatus()).isEqualTo(OrderStatus.PAID);

        verifyNoInteractions(transactionHistoryService);
        verifyNoInteractions(dashboardService);
    }

    @Test
    void complete_PG응답의_금액이_DB와_다르면_예외가_발생한다() {
        // given
        Refund refund = createProcessingRefund();

        when(refundRepository.findByIdWithPaymentAndOrder(REFUND_ID))
                .thenReturn(Optional.of(refund));

        PortOnePaymentResponse wrongAmountPayment = cancelledPaymentResponse(IMP_UID, BigDecimal.valueOf(9999));

        // when & then
        assertThatThrownBy(() -> adminRefundService.complete(REFUND_ID, wrongAmountPayment))
                .isInstanceOf(BaseException.class);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PROCESSING);
        assertThat(refund.getPayment().getStatus()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(refund.getPayment().getOrder().getStatus()).isEqualTo(OrderStatus.PAID);

        verifyNoInteractions(transactionHistoryService);
        verifyNoInteractions(dashboardService);
    }

    @Test
    void complete_이미_SUCCESS인_환불이면_멱등하게_SUCCESS를_반환한다() {
        // given
        Refund refund = createProcessingRefund();
        refund.success();

        when(refundRepository.findByIdWithPaymentAndOrder(REFUND_ID))
                .thenReturn(Optional.of(refund));

        // when
        AuthorizeRefundResponse response =
                adminRefundService.complete(REFUND_ID, null);

        // then
        assertThat(response.status()).isEqualTo(RefundStatus.SUCCESS);
        assertThat(refund.getStatus()).isEqualTo(RefundStatus.SUCCESS);

        verifyNoInteractions(transactionHistoryService);
        verifyNoInteractions(dashboardService);
    }

    @Test
    void complete_PROCESSING이_아닌_환불이면_예외가_발생한다() {
        // given
        Refund refund = createPendingRefund();

        when(refundRepository.findByIdWithPaymentAndOrder(REFUND_ID))
                .thenReturn(Optional.of(refund));

        // when & then
        assertThatThrownBy(() -> adminRefundService.complete(REFUND_ID, cancelledPaymentResponse(IMP_UID)))
                .isInstanceOf(BaseException.class);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.PENDING);

        verifyNoInteractions(transactionHistoryService);
        verifyNoInteractions(dashboardService);
    }

    @Test
    void complete_DENIED_환불이면_예외가_발생한다() {
        // given
        Refund refund = createPendingRefund();
        refund.deny("거절 사유");

        when(refundRepository.findByIdWithPaymentAndOrder(REFUND_ID))
                .thenReturn(Optional.of(refund));

        // when & then
        assertThatThrownBy(() -> adminRefundService.complete(REFUND_ID, cancelledPaymentResponse(IMP_UID)))
                .isInstanceOf(BaseException.class);

        assertThat(refund.getStatus()).isEqualTo(RefundStatus.DENIED);

        verifyNoInteractions(transactionHistoryService);
        verifyNoInteractions(dashboardService);
    }

    @Test
    void complete_환불건이_없으면_예외가_발생한다() {
        // given
        when(refundRepository.findByIdWithPaymentAndOrder(REFUND_ID))
                .thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminRefundService.complete(REFUND_ID, cancelledPaymentResponse(IMP_UID)))
                .isInstanceOf(BaseException.class);

        verifyNoInteractions(transactionHistoryService);
        verifyNoInteractions(dashboardService);
    }

    private Refund createProcessingRefund() {
        Refund refund = createPendingRefund();
        refund.processing();
        return refund;
    }

    private Refund createPendingRefund() {
        Buyer buyer = Buyer.of("buyer@test.com", "password", "구매자", "010-1234-5678");
        ReflectionTestUtils.setField(buyer, "id", 1L);

        Order order = Order.of(
                buyer,
                AMOUNT,
                null,
                "수령인",
                "010-1234-5678",
                "서울시"
        );
        ReflectionTestUtils.setField(order, "id", ORDER_ID);
        order.updateStatus(OrderStatus.PAID);

        Payment payment = Payment.of(order, "merchant_uid_123", AMOUNT, MethodEnum.MOCK);
        ReflectionTestUtils.setField(payment, "id", PAYMENT_ID);
        ReflectionTestUtils.setField(payment, "impUid", IMP_UID);
        payment.success(LocalDateTime.now());

        Refund refund = Refund.of(
                buyer,
                payment,
                ReasonEnum.CHANGE_OF_MIND,
                "환불 사유"
        );
        ReflectionTestUtils.setField(refund, "id", REFUND_ID);

        return refund;
    }

    private PortOnePaymentResponse cancelledPaymentResponse(String impUid) {
        return cancelledPaymentResponse(impUid, AMOUNT);
    }

    private PortOnePaymentResponse cancelledPaymentResponse(String impUid, BigDecimal totalAmount) {
        return paymentResponse("CANCELLED", impUid, totalAmount);
    }

    private PortOnePaymentResponse paidPaymentResponse(String impUid) {
        return paymentResponse("PAID", impUid, AMOUNT);
    }

    private PortOnePaymentResponse paymentResponse(String status, String impUid, BigDecimal totalAmount) {
        return new PortOnePaymentResponse(
                status,
                impUid,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                new PortOnePaymentResponse.Amount(
                        totalAmount,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        totalAmount,
                        BigDecimal.ZERO,
                        totalAmount,
                        "CANCELLED".equals(status) ? totalAmount : BigDecimal.ZERO,
                        BigDecimal.ZERO
                ),
                "KRW",
                null,
                null,
                null
        );
    }
}
