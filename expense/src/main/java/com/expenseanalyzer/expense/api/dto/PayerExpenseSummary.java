package com.expenseanalyzer.expense.api.dto;

import java.util.List;

public record PayerExpenseSummary(long paidBy, List<CurrencyExpenseTotal> totals) {
}
