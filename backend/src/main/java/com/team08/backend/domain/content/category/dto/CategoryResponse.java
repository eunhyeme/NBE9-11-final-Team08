package com.team08.backend.domain.content.category.dto;

import com.team08.backend.domain.content.category.entity.Category;

public record CategoryResponse(
        Long id,
        String name,
        Long parentCategoryId,
        Integer depth
) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(
                category.getId(),
                category.getName(),
                category.getParentCategoryId(),
                category.getDepth()
        );
    }
}
