package com.example.allinmarket.admin.seller.controller;

import com.example.allinmarket.admin.seller.dto.SellerStatusUpdateRequest;
import com.example.allinmarket.admin.seller.service.AdminSellerService;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.response.PageResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import com.example.allinmarket.seller.enums.SellerStatus;
import com.example.allinmarket.seller.me.dto.response.SellerDetailResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/sellers")
public class AdminSellerController {
    private final AdminSellerService adminSellerService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<SellerDetailResponse>>> findAllSeller(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable,
            @RequestParam(required = false)SellerStatus status
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.READ_SUCCESS,
                adminSellerService.findAllSeller(SecurityUtils.getCurrentUserId(), pageable, status)
                )
        );
    }

    @PutMapping("/{sellerId}/status")
    public ResponseEntity<ApiResponse<SellerDetailResponse>> updateSellerStatus(
            @PathVariable Long sellerId,
            @RequestBody SellerStatusUpdateRequest request
    ) {
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.UPDATE_SUCCESS,
                adminSellerService.updateSellerStatus(SecurityUtils.getCurrentUserId(), sellerId, request)
                )
        );
    }
}
