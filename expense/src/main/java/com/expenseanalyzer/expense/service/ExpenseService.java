package com.expenseanalyzer.expense.service;

import com.expenseanalyzer.expense.api.dto.CreateExpenseRequest;
import com.expenseanalyzer.expense.api.dto.ExpenseResponse;
import com.expenseanalyzer.expense.api.dto.ExpenseSummaryResponse;
import com.expenseanalyzer.expense.api.dto.CurrencyExpenseTotal;
import com.expenseanalyzer.expense.api.dto.PayerExpenseSummary;
import com.expenseanalyzer.expense.api.dto.UpdateExpenseRequest;
import com.expenseanalyzer.expense.domain.Expense;
import com.expenseanalyzer.expense.domain.Category;
import com.expenseanalyzer.expense.repository.CategoryRepository;
import com.expenseanalyzer.expense.repository.ExpenseRepository;
import com.expenseanalyzer.expense.repository.ExpenseSummaryRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;

@Service
@Transactional
public class ExpenseService {

    private final ExpenseRepository repository;
    private final CategoryRepository categoryRepository;

    public ExpenseService(ExpenseRepository repository, CategoryRepository categoryRepository) {
        this.repository = repository;
        this.categoryRepository = categoryRepository;
    }

    public ExpenseResponse create(CreateExpenseRequest request) {
        Expense expense = new Expense(
                normalizeDescription(request.description()), request.amount(), normalizeCurrency(request.currency()),
                request.expenseDate(), findCategory(request.categoryId()), normalizeNotes(request.notes()),
                request.paidBy(), request.isPersonal()
        );
        return ExpenseResponse.from(repository.saveAndFlush(expense));
    }

    @Transactional(readOnly = true)
    public Page<ExpenseResponse> list(LocalDate from, LocalDate to, Long categoryId, Long paidBy, Pageable pageable) {
        Specification<Expense> specification = (root, query, builder) -> builder.conjunction();
        if (from != null) {
            specification = specification.and((root, query, builder) -> builder.greaterThanOrEqualTo(root.get("expenseDate"), from));
        }
        if (to != null) {
            specification = specification.and((root, query, builder) -> builder.lessThanOrEqualTo(root.get("expenseDate"), to));
        }
        if (categoryId != null) {
            specification = specification.and((root, query, builder) ->
                    builder.equal(root.get("category").get("id"), categoryId));
        }
        if (paidBy != null) {
            specification = specification.and((root, query, builder) -> builder.equal(root.get("paidBy"), paidBy));
        }
        return repository.findAll(specification, pageable).map(ExpenseResponse::from);
    }

    @Transactional(readOnly = true)
    public ExpenseSummaryResponse summary(LocalDate from, LocalDate to) {
        Map<String, SummaryAccumulator> totalByCurrency = new LinkedHashMap<>();
        Map<String, SummaryAccumulator> sharedByCurrency = new LinkedHashMap<>();
        Map<String, SummaryAccumulator> personalByCurrency = new LinkedHashMap<>();
        Map<Long, PayerSummaryAccumulator> totalsByPayer = new LinkedHashMap<>();

        for (ExpenseSummaryRow row : repository.summarizeByPayerAndCurrency(from, to)) {
            totalByCurrency.computeIfAbsent(row.currency(), ignored -> new SummaryAccumulator())
                    .add(row.amount(), row.expenseCount());
            Map<String, SummaryAccumulator> bucketByCurrency = row.isPersonal()
                    ? personalByCurrency
                    : sharedByCurrency;
            bucketByCurrency.computeIfAbsent(row.currency(), ignored -> new SummaryAccumulator())
                    .add(row.amount(), row.expenseCount());
            totalsByPayer.computeIfAbsent(row.paidBy(), ignored -> new PayerSummaryAccumulator())
                    .add(row, row.isPersonal());
        }

        List<CurrencyExpenseTotal> totals = toTotals(totalByCurrency);
        List<CurrencyExpenseTotal> sharedTotals = toTotals(sharedByCurrency);
        List<CurrencyExpenseTotal> personalTotals = toTotals(personalByCurrency);
        List<PayerExpenseSummary> payers = totalsByPayer.entrySet().stream()
                .map(entry -> new PayerExpenseSummary(
                        entry.getKey(),
                        toTotals(entry.getValue().totals),
                        toTotals(entry.getValue().sharedTotals),
                        toTotals(entry.getValue().personalTotals)
                ))
                .toList();
        return new ExpenseSummaryResponse(totals, sharedTotals, personalTotals, payers);
    }

    private List<CurrencyExpenseTotal> toTotals(Map<String, SummaryAccumulator> totalsByCurrency) {
        return totalsByCurrency.entrySet().stream()
                .map(entry -> entry.getValue().toDto(entry.getKey()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ExpenseResponse get(long id) {
        return ExpenseResponse.from(findExpense(id));
    }

    public ExpenseResponse update(long id, UpdateExpenseRequest request) {
        Expense expense = findExpense(id);
        expense.replace(
                normalizeDescription(request.description()), request.amount(), normalizeCurrency(request.currency()),
                request.expenseDate(), findCategory(request.categoryId()), normalizeNotes(request.notes()),
                request.paidBy(), request.isPersonal()
        );
        return ExpenseResponse.from(repository.saveAndFlush(expense));
    }

    public void delete(long id) {
        repository.delete(findExpense(id));
    }

    private Expense findExpense(long id) {
        return repository.findById(id).orElseThrow(() -> new ExpenseNotFoundException(id));
    }

    private Category findCategory(long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private String normalizeCurrency(String currency) {
        return currency.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeDescription(String description) {
        return description == null ? null : description.trim();
    }

    private String normalizeNotes(String notes) {
        return notes == null || notes.isBlank() ? null : notes.trim();
    }

    private static final class SummaryAccumulator {
        private BigDecimal amount = BigDecimal.ZERO;
        private long expenseCount;

        private void add(BigDecimal amount, long expenseCount) {
            this.amount = this.amount.add(amount);
            this.expenseCount += expenseCount;
        }

        private CurrencyExpenseTotal toDto(String currency) {
            return new CurrencyExpenseTotal(currency, amount, expenseCount);
        }
    }

    private static final class PayerSummaryAccumulator {
        private final Map<String, SummaryAccumulator> totals = new LinkedHashMap<>();
        private final Map<String, SummaryAccumulator> sharedTotals = new LinkedHashMap<>();
        private final Map<String, SummaryAccumulator> personalTotals = new LinkedHashMap<>();

        private void add(ExpenseSummaryRow row, boolean personal) {
            totals.computeIfAbsent(row.currency(), ignored -> new SummaryAccumulator())
                    .add(row.amount(), row.expenseCount());
            Map<String, SummaryAccumulator> bucket = personal ? personalTotals : sharedTotals;
            bucket.computeIfAbsent(row.currency(), ignored -> new SummaryAccumulator())
                    .add(row.amount(), row.expenseCount());
        }
    }
}
