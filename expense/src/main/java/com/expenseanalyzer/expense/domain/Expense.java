package com.expenseanalyzer.expense.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "expenses")
public class Expense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String description;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "expense_date", nullable = false)
    private LocalDate expenseDate;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 2000)
    private String notes;

    @Column(name = "paid_by", nullable = false)
    private long paidBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private long version;

    protected Expense() {
    }

    public Expense(String description, BigDecimal amount, String currency, LocalDate expenseDate,
                   String category, String notes, long paidBy) {
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.expenseDate = expenseDate;
        this.category = category;
        this.notes = notes;
        this.paidBy = paidBy;
    }

    public void replace(String description, BigDecimal amount, String currency, LocalDate expenseDate,
                        String category, String notes, long paidBy) {
        this.description = description;
        this.amount = amount;
        this.currency = currency;
        this.expenseDate = expenseDate;
        this.category = category;
        this.notes = notes;
        this.paidBy = paidBy;
    }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now().truncatedTo(ChronoUnit.MICROS);
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now().truncatedTo(ChronoUnit.MICROS);
    }

    public Long getId() { return id; }
    public String getDescription() { return description; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public LocalDate getExpenseDate() { return expenseDate; }
    public String getCategory() { return category; }
    public String getNotes() { return notes; }
    public long getPaidBy() { return paidBy; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public long getVersion() { return version; }
}
