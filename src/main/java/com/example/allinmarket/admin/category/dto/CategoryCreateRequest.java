package com.example.allinmarket.admin.category.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import org.hibernate.validator.constraints.Length;

public record CategoryCreateRequest (
        @NotBlank(message = "카테고리 이름은 필수입니다.")
        @Length(max = 50, message = "카테고리 이름은 50자를 초과할 수 없습니다.")
        String name,

        @PositiveOrZero(message = "순서는 0 또는 양수로 지정해 주세요")
        int sortOrder
) {}

