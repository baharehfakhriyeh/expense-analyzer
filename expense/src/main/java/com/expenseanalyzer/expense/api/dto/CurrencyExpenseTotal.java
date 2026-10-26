package com.expenseanalyzer.expense.api.dto;

import java.math.BigDecimal;

public record CurrencyExpenseTotal(String currency, BigDecimal amount, long expenseCount) {
}
