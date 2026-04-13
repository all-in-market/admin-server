package com.example.allinmarket.admin.category.service;

import com.example.allinmarket.admin.category.dto.CategoryCreateRequest;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCategoryService {

    private final AdminRepository adminRepository;
    private final CategoryRepository categoryRepository;

    public List<CategoryDetailResponse> getCategories(Long adminId) {

        if (!adminRepository.existsByIdAndDeletedAtIsNull(adminId)) {
            throw new BaseException(ErrorEnum.ADMIN_NOT_FOUND);
        }

        return categoryRepository.findAllByDeletedAtIsNull().stream()
                .map(CategoryDetailResponse::from)
                .toList();
    }

    public CategoryDetailResponse create(Long adminId, CategoryCreateRequest request) {

        if (!adminRepository.existsByIdAndDeletedAtIsNull(adminId)) {
            throw new BaseException(ErrorEnum.ADMIN_NOT_FOUND);
        }

        Category createdCategory = Category.of(request.name(), request.sortOrder());
        categoryRepository.save(createdCategory);

        return CategoryDetailResponse.from(createdCategory);
    }
}
