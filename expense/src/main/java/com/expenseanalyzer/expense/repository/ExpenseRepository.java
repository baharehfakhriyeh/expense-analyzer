package com.expenseanalyzer.expense.repository;

import com.expenseanalyzer.expense.domain.Expense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long>, JpaSpecificationExecutor<Expense> {

    @Query("""
            select new com.expenseanalyzer.expense.repository.ExpenseSummaryRow(
                expense.paidBy, expense.currency, expense.personal,
                sum(expense.amount), count(expense.id)
            )
            from Expense expense
            where (:from is null or expense.expenseDate >= :from)
              and (:to is null or expense.expenseDate <= :to)
            group by expense.paidBy, expense.currency, expense.personal
            order by expense.paidBy, expense.currency, expense.personal
            """)
    List<ExpenseSummaryRow> summarizeByPayerAndCurrency(
            @Param("from") LocalDate from,
            @Param("to") LocalDate to
    );
}
