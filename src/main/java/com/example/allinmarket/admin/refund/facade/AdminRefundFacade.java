package com.example.allinmarket.admin.refund.facade;

import com.example.allinmarket.admin.refund.client.PaymentGateway;
import com.example.allinmarket.admin.refund.client.PortOnePaymentResponse;
import com.example.allinmarket.admin.refund.dto.response.AuthorizeRefundResponse;
import com.example.allinmarket.admin.refund.service.AdminRefundService;
import com.example.allinmarket.domain.refund.entity.Refund;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdminRefundFacade {

    private final AdminRefundService adminRefundService;
    private final PaymentGateway paymentGateway;

    public AuthorizeRefundResponse processRefund(Long adminId, Long refundId) {
        adminRefundService.validationForbidden(adminId);

        Refund refund = adminRefundService.getPendingRefundWithPayment(refundId);
        String impUid = refund.getPayment().getImpUid();

        // 외부 API - 포트원으로부터 결제 이력 조회
        PortOnePaymentResponse portOnePaymentResponse = paymentGateway.getPayment(impUid);

        // 환불 멱등성 처리 + 환불 가능한지 여부 확인
        AuthorizeRefundResponse authorizeRefundResponse = adminRefundService.checkPaymentRefundable(refund, portOnePaymentResponse);

        if (authorizeRefundResponse != null) {
            return authorizeRefundResponse;
        }

        // 외부 API - impUid 기준으로 포트원 전액 환불 요청
        // refundId를 통한 멱등키를 생성하여 중복 취소 요청 방지 (포트원 V2 REST API 필요)
        if (!portOnePaymentResponse.isCancelled()) {
            paymentGateway.cancelPayment(impUid, refundId);
        }

        // 외부 API - 포트원으로부터 결제 내역 재조회
        PortOnePaymentResponse canceledPayment = paymentGateway.getPayment(impUid);

        // 재조회한 내용을 바탕으로 환불이 제대로 됐는지 확인 후 환불 상태 db에 반영
        return adminRefundService.complete(refundId, canceledPayment);
    }
}
