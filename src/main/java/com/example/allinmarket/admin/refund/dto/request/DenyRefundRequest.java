package com.example.allinmarket.admin.refund.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record DenyRefundRequest(
        @NotBlank
        String deniedReason
){}
