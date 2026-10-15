package com.expenseanalyzer.expense.service;

public class CategoryNotFoundException extends RuntimeException {
    public CategoryNotFoundException(long id) {
        super("Category with id " + id + " was not found");
    }
}
