package com.expenseanalyzer.expense.service;

import com.expenseanalyzer.expense.api.dto.CategoryResponse;
import com.expenseanalyzer.expense.api.dto.CreateCategoryRequest;
import com.expenseanalyzer.expense.api.dto.UpdateCategoryRequest;
import com.expenseanalyzer.expense.domain.Category;
import com.expenseanalyzer.expense.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository repository;

    @InjectMocks
    private CategoryService service;

    @Test
    void createTrimsAndLowercasesNameBeforeSaving() {
        when(repository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CategoryResponse response = service.create(new CreateCategoryRequest("  FoOd  "));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("food");
        assertThat(response.name()).isEqualTo("food");
    }

    @Test
    void updateTrimsAndLowercasesNameBeforeSaving() {
        Category category = new Category("food");
        when(repository.findById(7L)).thenReturn(Optional.of(category));
        when(repository.saveAndFlush(category)).thenReturn(category);

        CategoryResponse response = service.update(7L, new UpdateCategoryRequest("  TRAVEL  "));

        assertThat(category.getName()).isEqualTo("travel");
        assertThat(response.name()).isEqualTo("travel");
        verify(repository).saveAndFlush(category);
    }

    @Test
    void listMapsAllCategoriesToResponses() {
        when(repository.findAll()).thenReturn(List.of(new Category("food"), new Category("travel")));

        List<CategoryResponse> responses = service.list();

        assertThat(responses).extracting(CategoryResponse::name).containsExactly("food", "travel");
    }

    @Test
    void getReturnsCategoryResponse() {
        when(repository.findById(3L)).thenReturn(Optional.of(new Category("food")));

        assertThat(service.get(3L).name()).isEqualTo("food");
    }

    @Test
    void getThrowsWhenCategoryDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(404L))
                .isInstanceOf(CategoryNotFoundException.class)
                .hasMessage("Category with id 404 was not found");
    }

    @Test
    void updateAndDeleteThrowWhenCategoryDoesNotExist() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(404L, new UpdateCategoryRequest("food")))
                .isInstanceOf(CategoryNotFoundException.class);
        assertThatThrownBy(() -> service.delete(404L))
                .isInstanceOf(CategoryNotFoundException.class);
    }

    @Test
    void deleteRemovesExistingCategory() {
        Category category = new Category("food");
        when(repository.findById(3L)).thenReturn(Optional.of(category));

        service.delete(3L);

        verify(repository).delete(category);
        verify(repository).flush();
    }
}
