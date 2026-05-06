package com.example.allinmarket.domain.order.event.dto;

import com.example.allinmarket.domain.order.enums.OrderStatus;
import com.example.allinmarket.domain.order.event.OrderStatusUpdateEvent;

public record OrderStatusUpdateEventRequest(
        Long orderId,
        OrderStatus status
) {
}
