package com.expenseanalyzer.expense.api.dto;

import com.expenseanalyzer.expense.domain.Expense;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate expenseDate,
        Long categoryId,
        String categoryName,
        String notes,
        long paidBy,
        @JsonProperty("isPersonal") boolean isPersonal,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(), expense.getDescription(), expense.getAmount(), expense.getCurrency(),
                expense.getExpenseDate(), expense.getCategory().getId(), expense.getCategory().getName(),
                expense.getNotes(), expense.getPaidBy(), expense.isPersonal(),
                expense.getCreatedAt(), expense.getUpdatedAt()
        );
    }
}
