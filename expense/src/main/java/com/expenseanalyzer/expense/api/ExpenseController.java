package com.expenseanalyzer.expense.api;

import com.expenseanalyzer.expense.api.dto.CreateExpenseRequest;
import com.expenseanalyzer.expense.api.dto.ExpenseResponse;
import com.expenseanalyzer.expense.api.dto.UpdateExpenseRequest;
import com.expenseanalyzer.expense.service.ExpenseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/expenses")
public class ExpenseController {

    private static final Set<String> SORTABLE_FIELDS = Set.of(
            "id", "description", "amount", "currency", "expenseDate", "category", "paidBy", "createdAt", "updatedAt"
    );

    private final ExpenseService expenseService;

    public ExpenseController(ExpenseService expenseService) {
        this.expenseService = expenseService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ExpenseResponse create(@Valid @RequestBody CreateExpenseRequest request) {
        return expenseService.create(request);
    }

    @GetMapping
    public Page<ExpenseResponse> list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) @Positive Long paidBy,
            @PageableDefault(size = 20, sort = "expenseDate") Pageable pageable
    ) {
        Set<String> unsupportedSortFields = pageable.getSort().stream()
                .map(order -> order.getProperty())
                .filter(property -> !SORTABLE_FIELDS.contains(property))
                .collect(Collectors.toSet());
        if (!unsupportedSortFields.isEmpty()) {
            throw new IllegalArgumentException("Unsupported sort field(s): " + String.join(", ", unsupportedSortFields));
        }
        if (from != null && to != null && from.isAfter(to)) {
            throw new IllegalArgumentException("from must be on or before to");
        }
        return expenseService.list(from, to, category, paidBy, pageable);
    }

    @GetMapping("/{id}")
    public ExpenseResponse get(@PathVariable @Positive long id) {
        return expenseService.get(id);
    }

    @PutMapping("/{id}")
    public ExpenseResponse update(@PathVariable @Positive long id, @Valid @RequestBody UpdateExpenseRequest request) {
        return expenseService.update(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable @Positive long id) {
        expenseService.delete(id);
    }
}
