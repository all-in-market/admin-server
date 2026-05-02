package com.example.allinmarket.admin.refund.client;

public interface PaymentGateway {
    PortOnePaymentResponse getPayment(String paymentId);
    void cancelPayment(String impUid, Long refundId);
}
