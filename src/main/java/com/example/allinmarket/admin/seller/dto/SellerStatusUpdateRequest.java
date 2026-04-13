package com.example.allinmarket.admin.seller.dto;

import com.example.allinmarket.seller.enums.SellerStatus;

public record SellerStatusUpdateRequest(
        SellerStatus status
) {
}
