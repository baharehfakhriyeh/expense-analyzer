package com.expenseanalyzer.expense.repository;

import java.math.BigDecimal;

public record ExpenseSummaryRow(long paidBy, String currency, BigDecimal amount, long expenseCount) {
}
