package com.example.allinmarket.admin.refund.dto.response;

import com.example.allinmarket.domain.refund.entity.Refund;
import com.example.allinmarket.domain.refund.enums.ReasonEnum;
import com.example.allinmarket.domain.transactionhistory.enums.TransactionStatus;

import java.time.LocalDateTime;

public record AuthorizeRefundResponse (
        Long id,
        Long buyerId,
        Long paymentId,
        ReasonEnum reason,
        String description,
        String deniedReason, //nullable
        TransactionStatus status,
        LocalDateTime processedAt //nullable
){
    public static AuthorizeRefundResponse from(Refund refund) {
        return new AuthorizeRefundResponse(
                refund.getId(),
                refund.getBuyer().getId(),
                refund.getPayment().getId(),
                refund.getReason(),
                refund.getDescription(),
                refund.getDeniedReason(),
                refund.getStatus(),
                refund.getProcessedAt()
        );
    }
}
