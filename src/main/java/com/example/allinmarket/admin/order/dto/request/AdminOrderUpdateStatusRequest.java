package com.example.allinmarket.admin.order.dto.request;

import com.example.allinmarket.domain.order.enums.OrderStatus;

public record AdminOrderUpdateStatusRequest(

        OrderStatus status
) {
}
