package com.expenseanalyzer.expense.api;

import com.expenseanalyzer.expense.domain.Expense;
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

import java.time.Instant;
import java.time.temporal.ChronoUnit;

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
class ExpenseApiIntegrationTest {

    private static final String ENDPOINT = "/api/v1/expenses";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExpenseRepository repository;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
    }

    @Test
    void createAndReadExpense() throws Exception {
        long id = createExpense("Groceries", "42.75", "2026-10-04", "Food", "usd", 1);

        mockMvc.perform(get(ENDPOINT + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Groceries"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.paidBy").value(1));
    }

    @Test
    void listFiltersByDateCategoryAndPaidByAndPaginates() throws Exception {
        createExpense("Market", "20.00", "2026-10-03", "Food", "USD", 1);
        createExpense("Fuel", "50.00", "2026-10-04", "Transport", "USD", 1);
        createExpense("Dinner", "30.00", "2026-10-04", "Food", "USD", 2);

        mockMvc.perform(get(ENDPOINT)
                        .param("from", "2026-10-04")
                        .param("to", "2026-10-04")
                        .param("category", " food ")
                        .param("paidBy", "2")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Dinner"));
    }

    @Test
    void updateReturnsPersistedUpdatedTimestamp() throws Exception {
        long id = createExpense("Old name", "8.00", "2026-10-04", "Other", "USD", 1);
        String originalUpdatedAt = repository.findById(id).orElseThrow().getUpdatedAt().toString();
        String body = """
                {
                  "description": "New name",
                  "amount": 9.50,
                  "currency": "eur",
                  "expenseDate": "2026-10-05",
                  "category": "Travel",
                  "notes": null,
                  "paidBy": 2
                }
                """;

        MvcResult result = mockMvc.perform(put(ENDPOINT + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("New name"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String responseUpdatedAt = response.get("updatedAt").asText();
        Expense persisted = repository.findById(id).orElseThrow();
        assertThat(responseUpdatedAt).isNotEqualTo(originalUpdatedAt);
        assertThat(Instant.parse(responseUpdatedAt).truncatedTo(ChronoUnit.MICROS))
                .isEqualTo(persisted.getUpdatedAt().truncatedTo(ChronoUnit.MICROS));
    }

    @Test
    void deleteReturnsNoContentAndSubsequentReadReturnsNotFound() throws Exception {
        long id = createExpense("To delete", "1.00", "2026-10-04", "Other", "USD", 1);

        mockMvc.perform(delete(ENDPOINT + "/{id}", id))
                .andExpect(status().isNoContent());
        mockMvc.perform(get(ENDPOINT + "/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void invalidRequestReturnsFieldValidationErrors() throws Exception {
        String body = """
                {
                  "description": "",
                  "amount": -1,
                  "currency": "USD",
                  "expenseDate": "2026-10-04",
                  "category": "Food",
                  "paidBy": 1
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.description").exists())
                .andExpect(jsonPath("$.details.amount").exists());
    }

    @Test
    void invalidCurrencyReturnsBadRequest() throws Exception {
        String body = """
                {
                  "description": "Invalid currency",
                  "amount": 1.00,
                  "currency": "ZZZ",
                  "expenseDate": "2026-10-04",
                  "category": "Other",
                  "paidBy": 1
                }
                """;

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.currency").exists());
    }

    @Test
    void invalidDateRangeAndSortFieldReturnBadRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("from", "2026-10-05").param("to", "2026-10-04"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(ENDPOINT).param("sort", "unknownField"))
                .andExpect(status().isBadRequest());
    }

    private long createExpense(String description, String amount, String date, String category,
                               String currency, long paidBy) throws Exception {
        String body = """
                {
                  "description": "%s",
                  "amount": %s,
                  "currency": "%s",
                  "expenseDate": "%s",
                  "category": "%s",
                  "notes": "  note  ",
                  "paidBy": %d
                }
                """.formatted(description, amount, currency, date, category, paidBy);
        MvcResult result = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }
}
