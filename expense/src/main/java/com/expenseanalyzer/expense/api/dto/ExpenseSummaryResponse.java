package com.expenseanalyzer.expense.api.dto;

import java.util.List;

public record ExpenseSummaryResponse(
        List<CurrencyExpenseTotal> totals,
        List<PayerExpenseSummary> payers
) {
}
