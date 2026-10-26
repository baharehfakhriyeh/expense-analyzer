package com.expenseanalyzer.expense.repository;

import com.expenseanalyzer.expense.domain.Category;
import com.expenseanalyzer.expense.domain.Expense;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("test")
class CategoryPersistenceIntegrationTest {

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void repositoryCanFindCategoryByItsNormalizedName() {
        Category saved = categoryRepository.saveAndFlush(new Category("food"));

        var found = categoryRepository.findByName("food");
        assertThat(found).isPresent();
        assertThat(found.orElseThrow().getId()).isEqualTo(saved.getId());
        assertThat(found.orElseThrow().getName()).isEqualTo("food");
        assertThat(categoryRepository.findByName("missing")).isEmpty();
    }

    @Test
    void databaseRejectsNamesThatAreNotLowercaseOrTrimmed() {
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO categories (name) VALUES (?)", "Food"))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThatThrownBy(() -> jdbcTemplate.update(
                "INSERT INTO categories (name) VALUES (?)", " food "))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void databaseRejectsDuplicateCategoryNames() {
        categoryRepository.saveAndFlush(new Category("food"));

        assertThatThrownBy(() -> categoryRepository.saveAndFlush(new Category("food")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    void expenseForeignKeyPreventsDeletingReferencedCategory() {
        Category category = categoryRepository.saveAndFlush(new Category("food"));
        expenseRepository.saveAndFlush(new Expense(
                "Lunch", new BigDecimal("12.50"), "USD", LocalDate.parse("2026-10-15"),
                category, null, 1L, false
        ));

        assertThatThrownBy(() -> jdbcTemplate.update(
                "DELETE FROM categories WHERE id = ?", category.getId()))
                .isInstanceOf(DataIntegrityViolationException.class);
        assertThat(categoryRepository.findById(category.getId())).isPresent();
    }
}
