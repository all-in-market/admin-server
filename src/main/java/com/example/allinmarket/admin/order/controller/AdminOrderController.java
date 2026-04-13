package com.example.allinmarket.admin.order.controller;

import com.example.allinmarket.admin.order.dto.request.AdminOrderUpdateStatusRequest;
import com.example.allinmarket.admin.order.service.AdminOrderService;
import com.example.allinmarket.buyer.order.dto.response.OrderDetailResponse;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/orders")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<OrderDetailResponse>>> findAll(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.READ_SUCCESS, adminOrderService.findAll(pageable)));
    }

    @PutMapping("/{orderId}/status")
    public ResponseEntity<ApiResponse<OrderDetailResponse>> updateStatus(
            @PathVariable(name = "orderId") Long orderId,
            @Valid @RequestBody AdminOrderUpdateStatusRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.UPDATE_SUCCESS, adminOrderService.updateStaus(orderId, request)));
    }
}
