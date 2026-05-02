package com.example.allinmarket.admin.refund.client;

import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.payment.entity.Payment;
import com.example.allinmarket.domain.payment.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
@Profile("!prod")
public class MockPaymentGateway implements PaymentGateway {

    private final Set<String> cancelledImpUids = ConcurrentHashMap.newKeySet();
    private final Set<String> cancelledIdempotentKeys = ConcurrentHashMap.newKeySet();
    private final PaymentRepository paymentRepository;

    @Override
    public PortOnePaymentResponse getPayment(String impUid) {
        Payment payment = paymentRepository.findByImpUid(impUid).orElseThrow(
                () -> new BaseException(ErrorEnum.PAYMENT_NOT_FOUND)
        );

        String status = cancelledImpUids.contains(payment.getImpUid()) ? "CANCELLED" : "PAID";

        return from(payment, status);
    }

    @Override
    public void cancelPayment(String impUid, Long refundId) {
        Payment payment = paymentRepository.findByImpUid(impUid).orElseThrow(
                () -> new BaseException(ErrorEnum.PAYMENT_NOT_FOUND)
        );

        String key = impUid + ":" + refundId;

        // 같은 멱등키 재요청이면 성공처럼 처리
        boolean firstIdempotentRequest = cancelledIdempotentKeys.add(key);
        if (!firstIdempotentRequest) {
            return;
        }

        // 다른 멱등키인데 이미 취소된 결제면 중복 환불
        boolean firstCancelRequest = cancelledImpUids.add(payment.getImpUid());
        if (!firstCancelRequest) {
            throw new BaseException(ErrorEnum.PAYMENT_ALREADY_REFUNDED);
        }
    }

    private PortOnePaymentResponse from(Payment payment, String status) {

        BigDecimal amount = payment.getAmount();

        PortOnePaymentResponse.Amount responseAmount =
                new PortOnePaymentResponse.Amount(
                        amount,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO,
                        amount,
                        BigDecimal.ZERO,
                        BigDecimal.ZERO
                );

        String now = now();

        return new PortOnePaymentResponse(
                status,
                payment.getImpUid(),
                "tx_" + payment.getImpUid(),
                payment.getMerchantUid(),
                "store_mock",
                null,
                null,
                "v1",
                now,
                now,
                now,
                "mock order",
                responseAmount,
                "KRW",
                null,
                now,
                "pg_tx_mock"
        );
    }

    private String now() {
        return String.valueOf(System.currentTimeMillis());
    }
}
