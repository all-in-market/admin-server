package com.example.allinmarket.admin.category.service;

import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminCategoryService {

    private final AdminRepository adminRepository;
    private final CategoryRepository categoryRepository;

    public List<CategoryDetailResponse> getCategories(Long adminId) {

        if (!adminRepository.existsByIdAndDeletedAtIsNull(adminId)) {
            throw new BaseException(ErrorEnum.ADMIN_NOT_FOUND);
        }

        return categoryRepository.findAllAndDeletedAtIsNull().stream()
                .map(CategoryDetailResponse::from)
                .toList();
    }
}
