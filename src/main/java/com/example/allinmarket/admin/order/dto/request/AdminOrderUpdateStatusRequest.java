package com.example.allinmarket.admin.order.dto.request;

import com.example.allinmarket.domain.order.enums.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record AdminOrderUpdateStatusRequest(

        @NotNull
        OrderStatus status
) {
}
