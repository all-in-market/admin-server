package com.example.allinmarket.admin.category.service;

import com.example.allinmarket.admin.category.dto.CategoryUpdateRequest;
import com.example.allinmarket.admin.repository.AdminRepository;
import com.example.allinmarket.buyer.category.dto.CategoryDetailResponse;
import com.example.allinmarket.common.enums.ErrorEnum;
import com.example.allinmarket.common.exception.BaseException;
import com.example.allinmarket.domain.category.entity.Category;
import com.example.allinmarket.domain.category.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class AdminCategoryServiceTest {

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private AdminCategoryService adminCategoryService;

    @Test
    void 카테고리_수정_성공_테스트() {
        // given
        Long adminId = 1L;
        Long categoryId = 10L;
        CategoryUpdateRequest request = new CategoryUpdateRequest("가전제품", 2);

        Category category = Category.of("전자제품", 1);
        ReflectionTestUtils.setField(category, "id", categoryId);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);
        given(categoryRepository.findByIdAndDeletedAtIsNull(categoryId)).willReturn(Optional.of(category));

        // when
        CategoryDetailResponse response = adminCategoryService.update(adminId, categoryId, request);

        // then
        assertThat(response.id()).isEqualTo(categoryId);
        assertThat(response.name()).isEqualTo("가전제품");
        assertThat(response.sortOrder()).isEqualTo(2);
    }

    @Test
    void 카테고리_수정_name_null이면_기존_이름_유지_테스트() {
        // given
        Long adminId = 1L;
        Long categoryId = 10L;
        CategoryUpdateRequest request = new CategoryUpdateRequest(null, 5);

        Category category = Category.of("전자제품", 1);
        ReflectionTestUtils.setField(category, "id", categoryId);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);
        given(categoryRepository.findByIdAndDeletedAtIsNull(categoryId)).willReturn(Optional.of(category));

        // when
        CategoryDetailResponse response = adminCategoryService.update(adminId, categoryId, request);

        // then
        assertThat(response.name()).isEqualTo("전자제품");
        assertThat(response.sortOrder()).isEqualTo(5);
    }

    @Test
    void 카테고리_수정_존재하지_않는_관리자_예외_테스트() {
        // given
        Long adminId = 999L;
        Long categoryId = 10L;
        CategoryUpdateRequest request = new CategoryUpdateRequest("가전제품", 2);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> adminCategoryService.update(adminId, categoryId, request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.ADMIN_NOT_FOUND.getMessage());
    }

    @Test
    void 카테고리_수정_존재하지_않는_카테고리_예외_테스트() {
        // given
        Long adminId = 1L;
        Long categoryId = 999L;
        CategoryUpdateRequest request = new CategoryUpdateRequest("가전제품", 2);

        given(adminRepository.existsByIdAndDeletedAtIsNull(adminId)).willReturn(true);
        given(categoryRepository.findByIdAndDeletedAtIsNull(categoryId)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> adminCategoryService.update(adminId, categoryId, request))
                .isInstanceOf(BaseException.class)
                .hasMessageContaining(ErrorEnum.CATEGORY_NOT_FOUND.getMessage());
    }
}
