package com.expenseanalyzer.expense.api.dto;

import com.expenseanalyzer.expense.domain.Expense;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseResponse(
        Long id,
        String description,
        BigDecimal amount,
        String currency,
        LocalDate expenseDate,
        String category,
        String notes,
        long paidBy,
        Instant createdAt,
        Instant updatedAt
) {
    public static ExpenseResponse from(Expense expense) {
        return new ExpenseResponse(
                expense.getId(), expense.getDescription(), expense.getAmount(), expense.getCurrency(),
                expense.getExpenseDate(), expense.getCategory(), expense.getNotes(), expense.getPaidBy(),
                expense.getCreatedAt(), expense.getUpdatedAt()
        );
    }
}
