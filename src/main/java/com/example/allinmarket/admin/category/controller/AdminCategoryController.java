package com.example.allinmarket.admin.category.controller;

import com.example.allinmarket.admin.category.dto.CategoryCreateRequest;
import com.example.allinmarket.admin.category.dto.CategoryUpdateRequest;
import com.example.allinmarket.admin.category.service.AdminCategoryService;
import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final AdminCategoryService adminCategoryService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CategoryDetailResponse>>> getCategories() {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.READ_SUCCESS,
                adminCategoryService.getCategories(adminId)
        ));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> create(
            @Valid @RequestBody CategoryCreateRequest request) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        SuccessEnum.CREATE_SUCCESS,
                        adminCategoryService.create(adminId, request)
                ));
    }

    @PutMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> update(
            @PathVariable Long categoryId,
            @Valid @RequestBody CategoryUpdateRequest request
    ) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(
                SuccessEnum.UPDATE_SUCCESS,
                adminCategoryService.update(adminId, categoryId, request)
        ));
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<ApiResponse<CategoryDetailResponse>> delete(
            @PathVariable Long categoryId
    ) {
        Long adminId = SecurityUtils.getCurrentUserId();
        return ResponseEntity.ok(ApiResponse.success(SuccessEnum.DELETE_SUCCESS, adminCategoryService.delete(adminId, categoryId)));
    }

}
