package com.expenseanalyzer.expense.api.dto;

import com.expenseanalyzer.expense.domain.Category;

public record CategoryResponse(Long id, String name) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName());
    }
}
