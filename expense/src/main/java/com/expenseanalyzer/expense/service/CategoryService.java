package com.expenseanalyzer.expense.service;

import com.expenseanalyzer.expense.api.dto.CategoryResponse;
import com.expenseanalyzer.expense.api.dto.CreateCategoryRequest;
import com.expenseanalyzer.expense.api.dto.UpdateCategoryRequest;
import com.expenseanalyzer.expense.domain.Category;
import com.expenseanalyzer.expense.repository.CategoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;

@Service
@Transactional
public class CategoryService {

    private final CategoryRepository repository;

    public CategoryService(CategoryRepository repository) {
        this.repository = repository;
    }

    public CategoryResponse create(CreateCategoryRequest request) {
        return CategoryResponse.from(repository.saveAndFlush(new Category(normalizeName(request.name()))));
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> list() {
        return repository.findAll().stream().map(CategoryResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CategoryResponse get(long id) {
        return CategoryResponse.from(findCategory(id));
    }

    public CategoryResponse update(long id, UpdateCategoryRequest request) {
        Category category = findCategory(id);
        category.rename(normalizeName(request.name()));
        return CategoryResponse.from(repository.saveAndFlush(category));
    }

    public void delete(long id) {
        repository.delete(findCategory(id));
        repository.flush();
    }

    private Category findCategory(long id) {
        return repository.findById(id).orElseThrow(() -> new CategoryNotFoundException(id));
    }

    private String normalizeName(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }
}
