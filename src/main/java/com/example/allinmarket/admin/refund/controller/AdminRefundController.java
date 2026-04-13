package com.example.allinmarket.admin.refund.controller;

import com.example.allinmarket.admin.refund.dto.request.AuthorizeRefundRequest;import com.example.allinmarket.admin.refund.dto.response.AuthorizeRefundResponse;
import com.example.allinmarket.admin.refund.service.AdminRefundService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.shaded.com.google.protobuf.Api;import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/refunds")
public class AdminRefundController {

    private final AdminRefundService adminRefundService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuthorizeRefundResponse>>> findAll(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.READ_SUCCESS, adminRefundService.findAll(adminId, pageable)));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<AuthorizeRefundResponse>> authorizeRefund(
            @PathVariable Long refundId,
            @RequestBody AuthorizeRefundRequest request
    ) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.UPDATE_SUCCESS,
                adminRefundService.authorizeRefund(adminId, refundId, request)
        ));
    }

}
