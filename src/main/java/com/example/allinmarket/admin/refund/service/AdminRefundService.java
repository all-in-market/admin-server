package com.example.allinmarket.admin.refund.service;

import com.example.allinmarket.admin.refund.dto.request.AuthorizeRefundRequest;
import com.example.allinmarket.admin.refund.dto.response.AuthorizeRefundResponse;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.repository.RefundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
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

    public AuthorizeRefundResponse authorizeRefund(Long adminId, Long refundId, AuthorizeRefundRequest request) {

        validationForbidden(adminId);

        Refund refund = refundRepository.findById(refundId).orElseThrow(
                () -> new BaseException(ErrorEnum.REFUND_NOT_FOUND)
        );





    }

    private void validationForbidden(Long adminId) {
        if (!adminRepository.existsByIdAndDeletedAtIsNull(adminId)) {
            throw new BaseException(ErrorEnum.ADMIN_NOT_FOUND);
        }
    }
}
