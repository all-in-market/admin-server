package com.example.allinmarket.admin.refund.service;

import com.example.allinmarket.admin.refund.dto.request.DenyRefundRequest;
import com.example.allinmarket.admin.refund.dto.response.AuthorizeRefundResponse;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.repository.RefundRepository;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminRefundService {

    private final RefundRepository refundRepository;
    private final AdminRepository adminRepository;

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

        Refund refund = refundRepository.findByIdAndStatus(refundId, TransactionStatus.PENDING).orElseThrow(
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

        Refund refund = refundRepository.findByIdAndStatus(refundId, TransactionStatus.PENDING).orElseThrow(
                () -> new BaseException(ErrorEnum.REFUND_NOT_FOUND)
        );

        // 외부 API - 환불 요청 refund.getPayment.getImpUid();

        refund.complete();
        refund.getPayment().cancel();
        refund.getPayment().getOrder().updateStatus(OrderStatus.REFUNDED);

        return AuthorizeRefundResponse.from(refund);

    }

    private void validationForbidden(Long adminId) {
        if (!adminRepository.existsByIdAndDeletedAtIsNull(adminId)) {
            throw new BaseException(ErrorEnum.ADMIN_NOT_FOUND);
        }
    }
}
