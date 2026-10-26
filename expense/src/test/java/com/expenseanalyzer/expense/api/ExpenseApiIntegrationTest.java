package com.expenseanalyzer.expense.api;

import com.expenseanalyzer.expense.domain.Expense;
import com.expenseanalyzer.expense.domain.Category;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.Locale;

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

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearDatabase() {
        repository.deleteAll();
        categoryRepository.deleteAll();
    }

    @Test
    void flywayMigrationRemovesCategoryIndex() {
        Integer matchingIndexes = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM INFORMATION_SCHEMA.INDEXES
                WHERE LOWER(TABLE_NAME) = 'expenses'
                  AND LOWER(INDEX_NAME) = 'idx_expenses_category'
                """, Integer.class);

        assertThat(matchingIndexes).isZero();
    }

    @Test
    void createAndReadExpense() throws Exception {
        long id = createExpense("Groceries", "42.75", "2026-10-04", "Food", "usd", 1);

        mockMvc.perform(get(ENDPOINT + "/{id}", id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.description").value("Groceries"))
                .andExpect(jsonPath("$.currency").value("USD"))
                .andExpect(jsonPath("$.categoryId").value(createCategory("Food")))
                .andExpect(jsonPath("$.categoryName").value("food"))
                .andExpect(jsonPath("$.paidBy").value(1))
                .andExpect(jsonPath("$.isPersonal").value(false));
    }

    @Test
    void createAndUpdateExpenseWithNullDescription() throws Exception {
        String createBody = """
                {
                  "description": null,
                  "amount": 8.00,
                  "currency": "USD",
                  "expenseDate": "2026-10-04",
                  "categoryId": %d,
                  "paidBy": 1,
                  "isPersonal": false
                }
                """.formatted(createCategory("Other"));
        MvcResult createResult = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andReturn();
        long id = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asLong();

        String updateBody = """
                {
                  "description": null,
                  "amount": 9.00,
                  "currency": "USD",
                  "expenseDate": "2026-10-05",
                  "categoryId": %d,
                  "paidBy": 1,
                  "isPersonal": true
                }
                """.formatted(createCategory("Other"));
        mockMvc.perform(put(ENDPOINT + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.isPersonal").value(true));

        assertThat(repository.findById(id).orElseThrow().getDescription()).isNull();
    }

    @Test
    void listFiltersByDateCategoryAndPaidByAndPaginates() throws Exception {
        createExpense("Market", "20.00", "2026-10-03", "Food", "USD", 1);
        createExpense("Fuel", "50.00", "2026-10-04", "Transport", "USD", 1);
        createExpense("Dinner", "30.00", "2026-10-04", "Food", "USD", 2);

        mockMvc.perform(get(ENDPOINT)
                        .param("from", "2026-10-04")
                        .param("to", "2026-10-04")
                        .param("categoryId", Long.toString(createCategory("Food")))
                        .param("paidBy", "2")
                        .param("page", "0")
                        .param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].description").value("Dinner"));
    }

    @Test
    void summarySeparatesSharedPersonalAndTotalByCurrencyAndPayer() throws Exception {
        createExpense("Shared one", "10.00", "2026-10-04", "Food", "USD", 1, false);
        createExpense("Personal one", "3.00", "2026-10-04", "Food", "USD", 1, true);
        createExpense("Shared euro", "4.00", "2026-10-04", "Travel", "EUR", 1, false);
        createExpense("Personal two", "7.00", "2026-10-04", "Food", "USD", 2, true);

        mockMvc.perform(get(ENDPOINT + "/summary")
                        .param("from", "2026-10-04")
                        .param("to", "2026-10-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totals[0].currency").value("EUR"))
                .andExpect(jsonPath("$.totals[0].amount").value(4.0))
                .andExpect(jsonPath("$.totals[1].currency").value("USD"))
                .andExpect(jsonPath("$.totals[1].amount").value(20.0))
                .andExpect(jsonPath("$.sharedTotals[0].amount").value(4.0))
                .andExpect(jsonPath("$.sharedTotals[1].amount").value(10.0))
                .andExpect(jsonPath("$.personalTotals").value(org.hamcrest.Matchers.hasSize(1)))
                .andExpect(jsonPath("$.personalTotals[0].currency").value("USD"))
                .andExpect(jsonPath("$.personalTotals[0].amount").value(10.0))
                .andExpect(jsonPath("$.payers", org.hamcrest.Matchers.hasSize(2)))
                .andExpect(jsonPath("$.payers[0].paidBy").value(1))
                .andExpect(jsonPath("$.payers[0].totals[1].amount").value(13.0))
                .andExpect(jsonPath("$.payers[0].sharedTotals[0].amount").value(4.0))
                .andExpect(jsonPath("$.payers[0].sharedTotals[1].amount").value(10.0))
                .andExpect(jsonPath("$.payers[0].personalTotals[0].amount").value(3.0))
                .andExpect(jsonPath("$.payers[1].paidBy").value(2))
                .andExpect(jsonPath("$.payers[1].personalTotals[0].amount").value(7.0));
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
                  "categoryId": %d,
                  "notes": null,
                  "paidBy": 2,
                  "isPersonal": true
                }
                """.formatted(createCategory("Travel"));

        MvcResult result = mockMvc.perform(put(ENDPOINT + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("New name"))
                .andExpect(jsonPath("$.currency").value("EUR"))
                .andExpect(jsonPath("$.categoryId").value(createCategory("Travel")))
                .andExpect(jsonPath("$.categoryName").value("travel"))
                .andExpect(jsonPath("$.isPersonal").value(true))
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        String responseUpdatedAt = response.get("updatedAt").asText();
        Expense persisted = repository.findById(id).orElseThrow();
        assertThat(responseUpdatedAt).isNotEqualTo(originalUpdatedAt);
        assertThat(Instant.parse(responseUpdatedAt)).isEqualTo(persisted.getUpdatedAt());
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
                  "categoryId": %d,
                  "paidBy": 1,
                  "isPersonal": false
                }
                """.formatted(createCategory("Food"));

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.description").exists())
                .andExpect(jsonPath("$.details.amount").exists());
    }

    @Test
    void createAndReplaceRequirePersonalFlag() throws Exception {
        String createBody = """
                {
                  "description": "Groceries",
                  "amount": 8.00,
                  "currency": "USD",
                  "expenseDate": "2026-10-04",
                  "categoryId": %d,
                  "paidBy": 1
                }
                """.formatted(createCategory("Food"));

        mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));

        long id = createExpense("Existing", "9.00", "2026-10-04", "Food", "USD", 1);
        String updateBody = """
                {
                  "description": "Replacement",
                  "amount": 10.00,
                  "currency": "USD",
                  "expenseDate": "2026-10-05",
                  "categoryId": %d,
                  "paidBy": 1
                }
                """.formatted(createCategory("Food"));

        mockMvc.perform(put(ENDPOINT + "/{id}", id)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void invalidCurrencyReturnsBadRequest() throws Exception {
        String body = """
                {
                  "description": "Invalid currency",
                  "amount": 1.00,
                  "currency": "ZZZ",
                  "expenseDate": "2026-10-04",
                  "categoryId": %d,
                  "paidBy": 1,
                  "isPersonal": false
                }
                """.formatted(createCategory("Other"));

        mockMvc.perform(post(ENDPOINT).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.currency").exists());
    }

    @Test
    void invalidDateRangeAndSortFieldReturnBadRequest() throws Exception {
        mockMvc.perform(get(ENDPOINT).param("from", "2026-10-05").param("to", "2026-10-04"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(get(ENDPOINT).param("sort", "unknownField"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get(ENDPOINT).param("from", "not-a-date"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
        mockMvc.perform(get(ENDPOINT).param("paidBy", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void openApiContractAndSwaggerUiAreAvailable() throws Exception {
        MvcResult specResult = mockMvc.perform(get("/openapi.yaml"))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(specResult.getResponse().getContentAsString())
                .contains("openapi: 3.0.3", "/api/v1/expenses", "ExpenseWriteRequest");

        mockMvc.perform(get("/swagger-ui.html"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/swagger-ui/index.html"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/v3/api-docs/swagger-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/openapi.yaml"));
    }

    private long createExpense(String description, String amount, String date, String category,
                               String currency, long paidBy) throws Exception {
        return createExpense(description, amount, date, category, currency, paidBy, false);
    }

    private long createExpense(String description, String amount, String date, String category,
                               String currency, long paidBy, boolean isPersonal) throws Exception {
        long categoryId = createCategory(category);
        String body = """
                {
                  "description": "%s",
                  "amount": %s,
                  "currency": "%s",
                  "expenseDate": "%s",
                  "categoryId": %d,
                  "notes": "  note  ",
                  "paidBy": %d,
                  "isPersonal": %s
                }
                """.formatted(description, amount, currency, date, categoryId, paidBy, isPersonal);
        MvcResult result = mockMvc.perform(post(ENDPOINT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long createCategory(String name) {
        String normalizedName = name.trim().toLowerCase(Locale.ROOT);
        Category category = categoryRepository.findByName(normalizedName)
                .orElseGet(() -> categoryRepository.saveAndFlush(new Category(normalizedName)));
        return category.getId();
    }
}
