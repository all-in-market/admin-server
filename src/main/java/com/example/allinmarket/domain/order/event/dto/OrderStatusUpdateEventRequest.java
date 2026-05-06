package com.example.allinmarket.domain.order.event.dto;

import com.example.allinmarket.domain.order.enums.OrderStatus;

public record OrderStatusUpdateEventRequest(
        Long buyerId,
        Long orderId,
        OrderStatus status
) {
}
