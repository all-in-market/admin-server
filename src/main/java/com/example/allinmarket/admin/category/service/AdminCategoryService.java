package com.example.allinmarket.admin.category.service;

import com.example.allinmarket.admin.category.dto.CategoryCreateRequest;
import com.example.allinmarket.admin.category.dto.CategoryUpdateRequest;
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

    private void validateAdmin(Long adminId) {
        if (!adminRepository.existsByIdAndDeletedAtIsNull(adminId)) {
            throw new BaseException(ErrorEnum.ADMIN_NOT_FOUND);
        }
    }

    public List<CategoryDetailResponse> getCategories(Long adminId) {

        validateAdmin(adminId);

        return categoryRepository.findAllByDeletedAtIsNull().stream()
                .map(CategoryDetailResponse::from)
                .toList();
    }

    @Transactional
    public CategoryDetailResponse create(Long adminId, CategoryCreateRequest request) {

        validateAdmin(adminId);

        Category createdCategory = Category.of(request.name(), request.sortOrder());
        categoryRepository.save(createdCategory);

        return CategoryDetailResponse.from(createdCategory);
    }

    @Transactional
    public CategoryDetailResponse update(Long adminId, Long categoryId, CategoryUpdateRequest request) {

        validateAdmin(adminId);

        Category category = categoryRepository.findByIdAndDeletedAtIsNull(categoryId)
                .orElseThrow(() -> new BaseException(ErrorEnum.CATEGORY_NOT_FOUND));

        category.updateName(request.name());
        category.updateSortOrder(request.sortOrder());
        return CategoryDetailResponse.from(category);
    }
}
