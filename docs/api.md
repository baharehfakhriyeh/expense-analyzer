# REST API

The complete OpenAPI contract is in [`expense/src/main/resources/static/openapi.yaml`](../expense/src/main/resources/static/openapi.yaml). With the application running, use Swagger UI at `/swagger-ui.html` or retrieve the contract at `/openapi.yaml`.

## Base URL and media type

The API prefix is `/api/v1`. Expense endpoints are under `/api/v1/expenses`, and category endpoints are under `/api/v1/categories`. Requests and responses use JSON (`application/json`) unless the operation returns no content. Dates use ISO `YYYY-MM-DD`; timestamps use ISO 8601 UTC.

## Endpoints

| Method | Path | Success response | Purpose |
|---|---|---|---|
| `POST` | `/api/v1/categories` | `201 Created` + `CategoryResponse` | Create a category |
| `GET` | `/api/v1/categories` | `200 OK` + category array | List categories |
| `GET` | `/api/v1/categories/{id}` | `200 OK` + `CategoryResponse` | Retrieve one category |
| `PUT` | `/api/v1/categories/{id}` | `200 OK` + `CategoryResponse` | Rename a category |
| `DELETE` | `/api/v1/categories/{id}` | `204 No Content` | Delete an unused category |
| `POST` | `/api/v1/expenses` | `201 Created` + `ExpenseResponse` | Create an expense |
| `GET` | `/api/v1/expenses` | `200 OK` + paged expenses | List/filter expenses |
| `GET` | `/api/v1/expenses/summary` | `200 OK` + `ExpenseSummaryResponse` | Aggregate totals by currency and payer |
| `GET` | `/api/v1/expenses/{id}` | `200 OK` + `ExpenseResponse` | Retrieve one expense |
| `PUT` | `/api/v1/expenses/{id}` | `200 OK` + `ExpenseResponse` | Replace an expense |
| `DELETE` | `/api/v1/expenses/{id}` | `204 No Content` | Delete an expense |

## List, filters, and sorting

`GET /api/v1/expenses` accepts:

- `from`, `to`: inclusive date bounds; `from` must not be later than `to`.
- `categoryId`: positive numeric identifier to filter by. An identifier with no matching expenses returns an empty page.
- `paidBy`: positive numeric scalar identifier.
- `page`: zero-based page index, default `0`.
- `size`: page size, default `20`, maximum `100`.
- `sort`: repeatable `property,direction` value, for example `sort=expenseDate,desc`. Supported properties are `id`, `description`, `amount`, `currency`, `expenseDate`, `category.name`, `paidBy`, `createdAt`, and `updatedAt`. The default is `expenseDate` ascending.

The list response is Spring Data's paged JSON representation. It contains `content`, page and sort metadata, and total element/page counts. Clients should use the documented fields rather than depending on internal framework-only fields in `pageable`.

`GET /api/v1/expenses/summary` accepts the same optional inclusive `from` and `to` date bounds. It returns totals grouped by currency and totals grouped by payer and currency across all matching expenses; it is not paginated. Payers are returned only when they have matching expenses. Amounts in different currencies are never combined. The `paidBy` value is a client-supplied numeric identifier, not a resolved user record.

## Expense fields and validation

Create and replacement requests provide `description`, `amount`, `currency`, `expenseDate`, `categoryId`, `notes`, and `paidBy`.

- `description`: optional and nullable; when provided, it is trimmed, must contain a non-whitespace character, and may be up to 200 characters.
- `amount`: required positive decimal, at most 15 integer digits and 4 fractional digits.
- `currency`: required recognized ISO 4217 code, case-insensitive on input and uppercase in responses.
- `expenseDate`: required calendar date.
- `categoryId`: required positive identifier of an existing category.
- `notes`: optional, at most 2,000 characters; blank notes are stored as null.
- `paidBy`: required positive 64-bit integer. Authentication is not present, so the caller supplies this value.

Responses also include a generated `id`, `createdAt`, and `updatedAt`. Timestamps are normalized to microsecond precision to match PostgreSQL storage. The internal optimistic-lock version is not exposed.

Expense responses also include `categoryId` and `categoryName`. Category names are trimmed and converted to lowercase before storage. Names are unique after normalization, and each expense must reference an existing category.

Category create and rename requests provide a `name` between 1 and 80 characters. The API trims and lowercases the name. Creating a duplicate normalized name returns `409 Conflict`. A category referenced by an expense cannot be deleted and also returns `409 Conflict`.

## Errors

Handled errors use this shape:

```json
{
  "timestamp": "2026-10-06T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Request validation failed",
  "details": {
    "amount": "must be greater than 0"
  }
}
```

Documented statuses include `400 Bad Request` for invalid values/body, invalid date range, or unsupported sort; `404 Not Found` when an expense or category ID is absent; and `409 Conflict` for duplicate category names, deletion of a category in use, or another database integrity conflict. `details` maps field names to validation messages and is empty when no field details apply.

## Interactive testing

1. Start PostgreSQL and the application as described in [development and operations](development.md).
2. Open `/swagger-ui.html` on the application host and port.
3. Expand an operation, choose **Try it out**, enter a request, then execute it.

There is no authentication scheme configured in Swagger because authentication is not implemented. Since `paidBy` is client-controlled, use this API only in a trusted local/development environment until authorization is added.
