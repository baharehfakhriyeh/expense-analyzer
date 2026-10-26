package com.expenseanalyzer.expense.service;

import com.expenseanalyzer.expense.api.dto.CreateExpenseRequest;
import com.expenseanalyzer.expense.api.dto.ExpenseResponse;
import com.expenseanalyzer.expense.api.dto.UpdateExpenseRequest;
import com.expenseanalyzer.expense.domain.Category;
import com.expenseanalyzer.expense.domain.Expense;
import com.expenseanalyzer.expense.repository.CategoryRepository;
import com.expenseanalyzer.expense.repository.ExpenseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExpenseServiceTest {

    @Mock
    private ExpenseRepository repository;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private ExpenseService service;

    @Test
    void createTrimsTextNormalizesCurrencyAndPersistsExpense() {
        when(repository.saveAndFlush(any(Expense.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(categoryRepository.findById(10L)).thenReturn(Optional.of(new Category("food")));
        CreateExpenseRequest request = new CreateExpenseRequest(
                "  Groceries  ", new BigDecimal("42.7500"), " usd ", LocalDate.parse("2026-10-04"),
                10L, "  Weekly shop  ", 7L, true
        );

        ExpenseResponse response = service.create(request);

        ArgumentCaptor<Expense> captor = ArgumentCaptor.forClass(Expense.class);
        verify(repository).saveAndFlush(captor.capture());
        Expense saved = captor.getValue();
        assertThat(saved.getDescription()).isEqualTo("Groceries");
        assertThat(saved.getCurrency()).isEqualTo("USD");
        assertThat(saved.getCategory().getName()).isEqualTo("food");
        assertThat(saved.getNotes()).isEqualTo("Weekly shop");
        assertThat(saved.getPaidBy()).isEqualTo(7L);
        assertThat(response.isPersonal()).isTrue();
        assertThat(response.amount()).isEqualByComparingTo("42.7500");
    }

    @Test
    void getThrowsNotFoundWhenExpenseDoesNotExist() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(99L))
                .isInstanceOf(ExpenseNotFoundException.class)
                .hasMessage("Expense with id 99 was not found");
    }

    @Test
    void updateReplacesExpenseFields() {
        Category updatedCategory = new Category("travel");
        when(categoryRepository.findById(12L)).thenReturn(Optional.of(updatedCategory));
        Expense expense = new Expense("Old", new BigDecimal("1.00"), "USD", LocalDate.parse("2026-01-01"),
                new Category("other"), null, 1L, false);
        when(repository.findById(3L)).thenReturn(Optional.of(expense));
        when(repository.saveAndFlush(expense)).thenReturn(expense);
        UpdateExpenseRequest request = new UpdateExpenseRequest(
                "  New  ", new BigDecimal("19.99"), "eur", LocalDate.parse("2026-02-03"),
                12L, " ", 4L, true
        );

        ExpenseResponse response = service.update(3L, request);

        assertThat(response.description()).isEqualTo("New");
        assertThat(response.amount()).isEqualByComparingTo("19.99");
        assertThat(response.currency()).isEqualTo("EUR");
        assertThat(response.categoryName()).isEqualTo("travel");
        assertThat(response.notes()).isNull();
        assertThat(response.paidBy()).isEqualTo(4L);
        assertThat(response.isPersonal()).isTrue();
        verify(repository).saveAndFlush(expense);
    }

    @Test
    void deleteRemovesExistingExpense() {
        Expense expense = new Expense("Meal", new BigDecimal("12.00"), "USD", LocalDate.now(),
                new Category("food"), null, 2L, false);
        when(repository.findById(5L)).thenReturn(Optional.of(expense));

        service.delete(5L);

        verify(repository).delete(expense);
    }
}
