package com.example.allinmarket.domain.order.event;

import com.example.allinmarket.domain.order.enums.OrderStatus;

public record OrderStatusUpdateEvent(
        Long buyerId,
        Long orderId,
        OrderStatus status
) {
}
