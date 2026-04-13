package com.example.allinmarket.admin.category.controller;

import com.example.allinmarket.admin.category.service.AdminCategoryService;
import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.common.enums.SuccessEnum;
import com.example.allinmarket.common.response.ApiResponse;
import com.example.allinmarket.common.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
}
