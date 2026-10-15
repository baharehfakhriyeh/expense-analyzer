# Architecture

## System purpose and scope

Expense Analyzer stores and retrieves expense records through a versioned REST API. The current application is a single deployable Spring Boot application in the Maven `expense` module. A separately maintained browser UI consumes this service through the same public HTTP API; its source and deployment are outside this backend's scope.

Authentication is intentionally absent. Each expense stores `paidBy` as a scalar `long`, with no JPA association or foreign key to a person/authentication module. Clients submit this value directly. The API currently offers no user or family isolation.

## C4: System context

```mermaid
flowchart LR
    Member[Family member using a browser UI or API client]
    System[Expense Analyzer<br/>Record and query expenses]
    Member -->|HTTPS/JSON requests; Swagger UI for manual exploration| System
```

## C4: Container view

```mermaid
flowchart LR
    Client[API client or separately maintained browser UI]
    App[Expense module<br/>Java 21 · Spring Boot MVC<br/>REST API + Swagger UI]
    DB[(PostgreSQL<br/>expense and category records)]
    Client -->|HTTP/JSON| App
    App -->|JPA / JDBC| DB
```

The `expense` module contains the expense and category REST controllers, request/response DTOs, validation, application services, Spring Data JPA repositories, and `Expense` and `Category` entities. An expense references one required category through `category_id`; each category can be shared by multiple expenses. Flyway owns schema changes. Swagger UI is served by the application and loads the checked-in OpenAPI YAML contract.

## Request flow

1. `ExpenseController` and `CategoryController` bind HTTP requests and validate request fields.
2. `ExpenseService` resolves the submitted `categoryId`, normalizes expense text and currency, builds query specifications, and applies transaction boundaries. `CategoryService` trims and lowercases category names before saving them.
3. `ExpenseRepository` and `CategoryRepository` read or write their entities using Spring Data JPA; the database foreign key enforces the expense-to-category relationship.
4. `ApiExceptionHandler` formats handled validation, not-found, and database conflict errors as `ApiError` responses.

## Persistence and external dependencies

- PostgreSQL is the configured runtime database.
- Flyway migrations are under `expense/src/main/resources/db/migration`.
- H2 is used only for integration tests; it is configured in the `test` profile.
- The OpenAPI specification is `expense/src/main/resources/static/openapi.yaml`; Swagger UI uses it for “Try it out”.

## Current boundaries

- One Maven module and one deployable process.
- Two entities: `Expense` and `Category`.
- No Person/Family entity, authentication, authorization, or ownership enforcement.
- The browser UI is a separate application and is not included in this repository or backend deployment. It communicates with this service through the documented REST API.
- No API gateway or external identity provider.
