package com.expenseanalyzer.expense.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import com.expenseanalyzer.expense.api.validation.IsoCurrency;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateExpenseRequest(
        @Pattern(regexp = "(?s).*\\S.*") @Size(max = 200) String description,
        @NotNull @Positive @Digits(integer = 15, fraction = 4) BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) @IsoCurrency String currency,
        @NotNull LocalDate expenseDate,
        @NotNull @Positive Long categoryId,
        @Size(max = 2000) String notes,
        @NotNull @Positive Long paidBy,
        @JsonProperty("isPersonal") @NotNull Boolean isPersonal
) {
}
