package com.example.allinmarket.admin.refund.service;

import com.example.allinmarket.admin.refund.dto.request.DenyRefundRequest;
import com.example.allinmarket.admin.refund.dto.response.AuthorizeRefundResponse;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.enums.OrderStatus;
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
    public AuthorizeRefundResponse complete(Long adminId, Long refundId) {

        validationForbidden(adminId);

        Refund refund = refundRepository.findByIdAndStatus(refundId, RefundStatus.PENDING).orElseThrow(
                () -> new BaseException(ErrorEnum.REFUND_NOT_FOUND)
        );

        // 외부 API - 포트원으로부터 결제 이력 조회

        // 환불 멱등성 처리 + 환불 가능한지 여부 확인

        // 외부 API - paymentId 기준으로 포트원 전액 환불 요청

        // 외부 API - 포트원으로부터 결제 내역 재조회

        // 재조회한 내용을 바탕으로 환불이 제대로 됐는지 확인 후 환불 상태 db에 반영

        refund.complete();
        // TransactionHistory에 refund.complete() 내역 추가
        try {
            transactionHistoryService.saveRefundHistory(refund);
            log.info("환불 승인 이력 저장 성공: refundId = {}", refund.getId());
        } catch (Exception e) {
            log.error("환불 승인 이력 저장 실패: refundId = {}, reason = {}", refund.getId(), e.getMessage());
        }

        refund.getPayment().cancel();
        // TransactionHistory에 payment.cancel() 내역 추가
        try {
            transactionHistoryService.savePaymentHistory(refund.getPayment());
            log.info("결제 취소 이력 저장 성공: paymentId = {}", refund.getPayment().getId());
        } catch (Exception e) {
            log.error("결제 취소 이력 저장 실패: paymentId = {}, reason = {}", refund.getPayment().getId(), e.getMessage());
        }
        refund.getPayment().getOrder().updateStatus(OrderStatus.REFUNDED);
        // SellerDashboard에 주문 취소 내역 반영
        dashboardService.updateSellerDashboardWithRefund(refund.getPayment().getOrder().getId());

        return AuthorizeRefundResponse.from(refund);

    }

    private void validationForbidden(Long adminId) {
        if (!adminRepository.existsByIdAndDeletedAtIsNull(adminId)) {
            throw new BaseException(ErrorEnum.ADMIN_NOT_FOUND);
        }
    }
}
