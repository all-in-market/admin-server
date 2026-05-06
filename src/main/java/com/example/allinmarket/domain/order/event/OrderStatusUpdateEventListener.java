package com.example.allinmarket.domain.order.event;


import com.example.allinmarket.common.security.HmacSigner;
import com.example.allinmarket.domain.order.event.dto.OrderStatusUpdateEventRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Instant;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderStatusUpdateEventListener {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${notification-server.url}")
    private String notificationServerUrl;

    @Value("${notification.auth.client-id}")
    private String clientId;

    @Value("${notification.auth.secret}")
    private String secret;

    @Retryable(
            retryFor = RestClientException.class,
            maxAttempts = 4,
            backoff = @Backoff(multiplier = 2, random = true)
    )
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleOrderStatusUpdateEvent(OrderStatusUpdateEvent event) {
        try{
            OrderStatusUpdateEventRequest request = new OrderStatusUpdateEventRequest(event.buyerId(), event.orderId(), event.status());

            String body = objectMapper.writeValueAsString(request);

            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String requestId = UUID.randomUUID().toString();

            String signature = HmacSigner.sign(
                    secret,
                    timestamp + requestId + body
            );

            restClient.post()
                    .uri(notificationServerUrl + "/internal/notifications/orders")
                    .header("X-Client-Id", clientId)
                    .header("X-Timestamp", timestamp)
                    .header("X-Request-Id", requestId)
                    .header("X-Signature", signature)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException e) {
            log.error("주문 상태 변경 알림 전송 실패. buyerId = {}, orderId={}, status = {}", event.buyerId(), event.orderId(), event.status(), e);
            throw e;
        } catch (JsonProcessingException e) {
            log.error("주문 상태 변경 이벤트 직렬화 실패. orderId={}, status = {}", event.orderId(), event.status(), e);
        }
    }
}
