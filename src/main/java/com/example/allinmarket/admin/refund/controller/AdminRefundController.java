package com.example.allinmarket.admin.refund.controller;

import com.example.allinmarket.admin.refund.dto.request.DenyRefundRequest;
import com.example.allinmarket.admin.refund.dto.response.AuthorizeRefundResponse;
import com.example.allinmarket.admin.refund.facade.AdminRefundFacade;
import com.example.allinmarket.admin.refund.service.AdminRefundService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/refunds")
public class AdminRefundController {

    private final AdminRefundService adminRefundService;
    private final AdminRefundFacade adminRefundFacade;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AuthorizeRefundResponse>>> findAll(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable
    ) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.READ_SUCCESS, adminRefundService.findAll(adminId, pageable)));
    }

    @PutMapping("/{refundId}/deny")
    public ResponseEntity<ApiResponse<AuthorizeRefundResponse>> deny(
            @PathVariable Long refundId,
            @RequestBody @Valid DenyRefundRequest request
    ) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.UPDATE_SUCCESS,
                adminRefundService.deny(adminId, refundId, request)
        ));
    }

    @PutMapping("/{refundId}/complete")
    public ResponseEntity<ApiResponse<AuthorizeRefundResponse>> complete(
            @PathVariable Long refundId
    ) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.UPDATE_SUCCESS,
                adminRefundFacade.processRefund(adminId, refundId)
        ));
    }

}
