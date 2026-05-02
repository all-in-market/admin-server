package com.example.allinmarket.admin.refund.service;

import com.example.allinmarket.admin.refund.client.PortOnePaymentResponse;
import com.example.allinmarket.admin.refund.dto.request.DenyRefundRequest;
import com.example.allinmarket.admin.refund.dto.response.AuthorizeRefundResponse;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.enums.PaymentStatus;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.enums.RefundStatus;
import com.example.allinmarket.domain.refund.repository.RefundRepository;
import com.example.allinmarket.domain.sellerdashboard.service.DashboardService;
import com.example.allinmarket.domain.transactionhistory.service.TransactionHistoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class AdminRefundService {

    private final RefundRepository refundRepository;
    private final AdminRepository adminRepository;
    private final TransactionHistoryService transactionHistoryService;
    private final DashboardService dashboardService;

    public PageResponse<AuthorizeRefundResponse> findAll(Long adminId, Pageable pageable) {

        validationForbidden(adminId);

        return PageResponse.register(
                refundRepository.findAllBy(pageable)
                        .map(AuthorizeRefundResponse::from)
        );
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public AuthorizeRefundResponse deny(Long adminId, Long refundId, DenyRefundRequest request) {

        validationForbidden(adminId);

        Refund refund = refundRepository.findByIdAndStatus(refundId, RefundStatus.PENDING).orElseThrow(
                () -> new BaseException(ErrorEnum.REFUND_NOT_FOUND)
        );

        refund.deny(request.deniedReason());

        return AuthorizeRefundResponse.from(refund);
    }

    @Retryable(
            retryFor = OptimisticLockingFailureException.class,
            maxAttempts = 3,
            backoff = @Backoff(delay = 100)
    )
    @Transactional
    public AuthorizeRefundResponse complete(Long refundId, PortOnePaymentResponse canceledPayment) {

        Refund refund = refundRepository.findByIdWithPaymentAndOrder(refundId).orElseThrow(
                () -> new BaseException(ErrorEnum.REFUND_NOT_FOUND)
        );

        // 멱등성 처리
        if (refund.getStatus() == RefundStatus.SUCCESS) {
            return AuthorizeRefundResponse.from(refund);
        }

        if (refund.getStatus() != RefundStatus.PROCESSING) {
            throw new BaseException(ErrorEnum.REFUND_NOT_FOUND);
        }

        Payment dbPayment = refund.getPayment();

        if (canceledPayment == null) {
            throw new BaseException(ErrorEnum.PAYMENT_GATEWAY_ERROR);
        }

        if (!dbPayment.getImpUid().equals(canceledPayment.id())) {
            throw new BaseException(ErrorEnum.PAYMENT_NOT_REFUNDABLE);
        }

        if (!canceledPayment.isCancelled()) {
            refund.fail();
            return AuthorizeRefundResponse.from(refund);
        }

        if (canceledPayment.amount() == null
                || canceledPayment.amount().total() == null
                || dbPayment.getAmount().compareTo(canceledPayment.amount().total()) != 0) {
            throw new BaseException(ErrorEnum.PAYMENT_NOT_REFUNDABLE);
        }

        refund.success();

        transactionHistoryService.saveRefundHistory(refund);

        // TransactionHistory에 payment.cancel() 내역 추가
        dbPayment.cancel();
        transactionHistoryService.savePaymentHistory(refund.getPayment());

        // SellerDashboard에 주문 취소 내역 반영
        dbPayment.getOrder().updateStatus(OrderStatus.REFUNDED);
        dashboardService.updateSellerDashboardWithRefund(dbPayment.getOrder().getId());

        return AuthorizeRefundResponse.from(refund);
    }

    @Transactional
    public AuthorizeRefundResponse checkPaymentRefundable(Refund refund, PortOnePaymentResponse portOnePaymentResponse) {

        Payment dbPayment = refund.getPayment();

        // DB 기준 이미 환불 완료된 경우 멱등 응답
        AuthorizeRefundResponse idempotentRefundedResponse = handleAlreadyRefundedPayment(dbPayment);
        if (idempotentRefundedResponse != null) {
            return idempotentRefundedResponse;
        }

        // 환불 요청 상태 검증
        if (refund.getStatus() != RefundStatus.PROCESSING) {
            throw new BaseException(ErrorEnum.REFUND_NOT_FOUND);
        }

        // DB 결제 상태 검증
        if (dbPayment.getStatus() == PaymentStatus.PENDING) {
            throw new BaseException(ErrorEnum.PAYMENT_NOT_REFUNDABLE);
        }

        // PG 응답 검증
        if (portOnePaymentResponse == null) {
            throw new BaseException(ErrorEnum.PAYMENT_NOT_REFUNDABLE);
        }

        if (!dbPayment.getImpUid().equals(portOnePaymentResponse.id())) {
            throw new BaseException(ErrorEnum.PAYMENT_NOT_REFUNDABLE);
        }

        if (dbPayment.getAmount().compareTo(portOnePaymentResponse.amount().total()) != 0) {
            throw new BaseException(ErrorEnum.PAYMENT_NOT_REFUNDABLE);
        }

        // 이미 포트원에서 환불이 취소되었는데 complete 단계에서 실패한 경우 통과
        if (portOnePaymentResponse.isCancelled()) {
            return null;
        }

        // 아직 취소 전이면 결제는 완료 상태여야 함
        if (!portOnePaymentResponse.isPaid()) {
            throw new BaseException(ErrorEnum.PAYMENT_NOT_REFUNDABLE);
        }

        return null;
    }

    /**
     * 이미 환불 처리된 결제인지 멱등성 검사 (동일 결제 중복 환불 처리 방지)
     * 이미 환불된 결제인 경우 예외 대신 성공 응답메세지 전송
     */
    private AuthorizeRefundResponse handleAlreadyRefundedPayment(Payment dbPayment) {
        if (dbPayment.getStatus() == PaymentStatus.REFUNDED) {

            Refund refund = refundRepository.findByPayment(dbPayment).orElseThrow(
                    () -> new BaseException(ErrorEnum.REFUND_NOT_FOUND)
            );

            return AuthorizeRefundResponse.from(refund);
        }

        return null;
    }


    public void validationForbidden(Long adminId) {
        if (!adminRepository.existsByIdAndDeletedAtIsNull(adminId)) {
            throw new BaseException(ErrorEnum.ADMIN_NOT_FOUND);
        }
    }

    @Transactional
    public Refund startRefundProcessing(Long refundId) {

        Refund refund = refundRepository.findByIdWithPayment(refundId).orElseThrow(
                () -> new BaseException(ErrorEnum.REFUND_NOT_FOUND)
        );

        if (refund.getStatus() != RefundStatus.PENDING && refund.getStatus() != RefundStatus.FAILED) {
            throw new BaseException(ErrorEnum.REFUND_NOT_FOUND);
        }

        refund.processing();

        return refund;
    }

    @Transactional
    public void failProcessing(Long refundId) {
        Refund refund = refundRepository.findById(refundId).orElseThrow(
                () -> new BaseException(ErrorEnum.REFUND_NOT_FOUND)
        );

        if (refund.getStatus() == RefundStatus.PROCESSING) {
            refund.fail();
        }
    }
}
