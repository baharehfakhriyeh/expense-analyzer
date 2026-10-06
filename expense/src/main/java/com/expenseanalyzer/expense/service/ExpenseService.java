package com.expenseanalyzer.expense.service;

import com.expenseanalyzer.expense.api.dto.CreateExpenseRequest;
import com.expenseanalyzer.expense.api.dto.ExpenseResponse;
import com.expenseanalyzer.expense.api.dto.UpdateExpenseRequest;
import com.expenseanalyzer.expense.domain.Expense;
import com.expenseanalyzer.expense.repository.ExpenseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository repository;

    public ExpenseService(ExpenseRepository repository) {
        this.repository = repository;
    }

    public ExpenseResponse create(CreateExpenseRequest request) {
        Expense expense = new Expense(
                request.description().trim(), request.amount(), normalizeCurrency(request.currency()),
                request.expenseDate(), request.category().trim(), normalizeNotes(request.notes()), request.paidBy()
        );
        return ExpenseResponse.from(repository.saveAndFlush(expense));
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> list(LocalDate from, LocalDate to, String category, Long paidBy, Pageable pageable) {
        Specification<Expense> specification = (root, query, builder) -> builder.conjunction();
        if (from != null) {
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("expenseDate"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("expenseDate"), to));
        }
        if (category != null && !category.isBlank()) {
            String normalizedCategory = category.trim().toLowerCase(Locale.ROOT);
            specification = specification.and((root, query, builder) ->
                    builder.equal(builder.lower(root.get("category")), normalizedCategory));
        }
        if (paidBy != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("paidBy"), paidBy));
        }
        return repository.findAll(specification, pageable).map(ExpenseResponse::from);
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(long id) {
        return ExpenseResponse.from(findExpense(id));
    }

    public ExpenseResponse update(long id, UpdateExpenseRequest request) {
        Expense expense = findExpense(id);
        expense.replace(
                request.description().trim(), request.amount(), normalizeCurrency(request.currency()),
                request.expenseDate(), request.category().trim(), normalizeNotes(request.notes()), request.paidBy()
        );
        return ExpenseResponse.from(repository.saveAndFlush(expense));
    }

    public void delete(long id) {
        repository.delete(findExpense(id));
    }

    private Expense findExpense(long id) {
        return repository.findById(id).orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    private String normalizeCurrency(String currency) {
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }
}
