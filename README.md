# Expense Analyzer

Java 21 Maven multi-module project for recording expenses. The current project contains one runnable Spring Boot module, `expense`. Authentication is intentionally not configured; `paidBy` is a numeric `long` supplied directly on create and update requests until the existing authentication module is integrated.

## Requirements

- JDK 21
- Maven 3.6.3+
- Docker Compose (for the included PostgreSQL database)

## Run locally

Start PostgreSQL:

```shell
docker compose up -d postgres
```

Run the API from the project root:

```shell
mvn -pl expense spring-boot:run
```

The API listens on `http://localhost:8080`. Flyway creates the schema on startup. Database connection settings can be overridden with `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`; the server port can be changed with `SERVER_PORT`.

To create the executable JAR:

```shell
mvn -pl expense package
java -jar expense/target/expense-0.1.0-SNAPSHOT.jar
```

Run unit and integration tests:

```shell
mvn -pl expense test
```

Integration tests use an in-memory H2 database and apply the project's Flyway migration; they do not require Docker.

## REST API

All endpoints are under `/api/v1/expenses`.

| Method | Path | Result |
|---|---|---|
| `POST` | `/api/v1/expenses` | Create an expense (`201 Created`) |
| `GET` | `/api/v1/expenses` | List expenses, paginated |
| `GET` | `/api/v1/expenses/{id}` | Retrieve one expense |
| `PUT` | `/api/v1/expenses/{id}` | Replace an expense |
| `DELETE` | `/api/v1/expenses/{id}` | Delete an expense (`204 No Content`) |

List query parameters: `from`, `to` (ISO dates), `category`, `paidBy`, `page`, `size`, and `sort`. The maximum page size is 100.

Example create request:

```json
{
  "description": "Groceries",
  "amount": 42.75,
  "currency": "USD",
  "expenseDate": "2026-10-04",
  "category": "Food",
  "notes": "Weekly shopping",
  "paidBy": 1
}
```

Amounts must be positive and support up to four decimal places. Currency is normalized to uppercase. Dates use `YYYY-MM-DD`. Error responses provide a timestamp, HTTP status, message, and optional field-level validation details.

## Data model

The `Expense` entity contains `id`, `description`, `amount`, `currency`, `expenseDate`, `category`, optional `notes`, `paidBy` (`long`), `createdAt`, `updatedAt`, and an optimistic-lock version. `paidBy` is deliberately a scalar ID, not a JPA relationship, so this module has no dependency on an authentication/person module.
