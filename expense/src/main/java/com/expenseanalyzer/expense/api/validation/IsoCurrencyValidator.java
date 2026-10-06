package com.expenseanalyzer.expense.api.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Currency;
import java.util.Locale;

public class IsoCurrencyValidator implements ConstraintValidator<IsoCurrency, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true; // Requiredness is handled by @NotBlank.
        }
        try {
            Currency.getInstance(value.trim().toUpperCase(Locale.ROOT));
            return true;
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
