package com.expenseanalyzer.expense.api;

import com.expenseanalyzer.expense.repository.CategoryRepository;
import com.expenseanalyzer.expense.repository.ExpenseRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CategoryControllerIntegrationTest {

    private static final String CATEGORIES_ENDPOINT = "/api/v1/categories";
    private static final String EXPENSES_ENDPOINT = "/api/v1/expenses";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ExpenseRepository expenseRepository;

    @BeforeEach
    void clearDatabase() {
        expenseRepository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void categoryCrudNormalizesNamesAndProtectsCategoriesUsedByExpenses() throws Exception {
        MvcResult createResult = mockMvc.perform(post(CATEGORIES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  FoOd  "}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("food"))
                .andReturn();
        long categoryId = readId(createResult);

        mockMvc.perform(get(CATEGORIES_ENDPOINT + "/{id}", categoryId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(categoryId))
                .andExpect(jsonPath("$.name").value("food"));
        mockMvc.perform(get(CATEGORIES_ENDPOINT))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(categoryId))
                .andExpect(jsonPath("$[0].name").value("food"));

        mockMvc.perform(put(CATEGORIES_ENDPOINT + "/{id}", categoryId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "  TRAVEL  "}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("travel"));

        mockMvc.perform(post(CATEGORIES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": " Travel "}
                                """))
                .andExpect(status().isConflict());

        String createExpense = """
                {
                  "description": "Train ticket",
                  "amount": 12.50,
                  "currency": "USD",
                  "expenseDate": "2026-10-15",
                  "categoryId": %d,
                  "paidBy": 1,
                  "isPersonal": false
                }
                """.formatted(categoryId);
        MvcResult expenseResult = mockMvc.perform(post(EXPENSES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createExpense))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoryId").value(categoryId))
                .andExpect(jsonPath("$.categoryName").value("travel"))
                .andReturn();
        long expenseId = readId(expenseResult);

        mockMvc.perform(delete(CATEGORIES_ENDPOINT + "/{id}", categoryId))
                .andExpect(status().isConflict());

        mockMvc.perform(delete(EXPENSES_ENDPOINT + "/{id}", expenseId))
                .andExpect(status().isNoContent());
        mockMvc.perform(delete(CATEGORIES_ENDPOINT + "/{id}", categoryId))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(CATEGORIES_ENDPOINT + "/{id}", categoryId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category with id " + categoryId + " was not found"));
    }

    @Test
    void categoryRequestsRejectInvalidNamesAndUnknownIds() throws Exception {
        mockMvc.perform(post(CATEGORIES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());

        mockMvc.perform(post(CATEGORIES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(categoryBody("x".repeat(81))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").exists());

        mockMvc.perform(put(CATEGORIES_ENDPOINT + "/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "   "}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isNotEmpty());

        mockMvc.perform(get(CATEGORIES_ENDPOINT + "/{id}", 999))
                .andExpect(status().isNotFound());
        mockMvc.perform(put(CATEGORIES_ENDPOINT + "/{id}", 999)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name": "new"}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(CATEGORIES_ENDPOINT + "/{id}", 999))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(CATEGORIES_ENDPOINT + "/{id}", 0))
                .andExpect(status().isBadRequest());
    }

    @Test
    void expenseWriteReturnsNotFoundForUnknownCategoryId() throws Exception {
        String body = """
                {
                  "amount": 1.00,
                  "currency": "USD",
                  "expenseDate": "2026-10-15",
                  "categoryId": 999,
                  "paidBy": 1,
                  "isPersonal": false
                }
                """;

        mockMvc.perform(post(EXPENSES_ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Category with id 999 was not found"));
    }

    private String categoryBody(String name) throws Exception {
        return objectMapper.writeValueAsString(java.util.Map.of("name", name));
    }

    private long readId(MvcResult result) throws Exception {
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("id").asLong();
    }
}
