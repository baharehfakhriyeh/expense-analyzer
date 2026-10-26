package com.expenseanalyzer.expense.repository;

import java.math.BigDecimal;

public record ExpenseSummaryRow(
        long paidBy,
        String currency,
        boolean isPersonal,
        BigDecimal amount,
        long expenseCount
) {
}
