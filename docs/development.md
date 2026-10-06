# Development and operations

## Prerequisites

- JDK 21
- Maven 3.6.3 or later
- Docker Compose for local PostgreSQL

## Start the application

From the repository root:

```shell
docker compose up -d postgres
mvn -pl expense spring-boot:run
```

The committed default port is `8087`; set `SERVER_PORT` to use another port. The Swagger UI is `/swagger-ui.html` and the canonical OpenAPI file is `/openapi.yaml` on that port. Flyway applies migrations on startup.

To package and run the executable JAR:

```shell
mvn -pl expense package
java -jar expense/target/expense-0.1.0-SNAPSHOT.jar
```

## Configuration

| Variable | Purpose | Default in committed configuration                  |
|---|---|-----------------------------------------------------|
| `DATABASE_URL` | JDBC URL | `jdbc:postgresql://localhost:5432/expense_analyzer` |
| `DATABASE_USERNAME` | Database username | `expense`                                           |
| `DATABASE_PASSWORD` | Database password | `expense` (local Compose default)                   |
| `SERVER_PORT` | HTTP port | `8087`                                              |

Use environment-specific values outside local development; do not commit real credentials. The local Compose credentials are for development only.

## Tests

```shell
mvn -pl expense clean test
```

`ExpenseServiceTest` uses Mockito for service behavior. `ExpenseApiIntegrationTest` starts the Spring context, runs Flyway against in-memory H2, and exercises the REST API through MockMvc. The H2 compatibility mode is useful for fast integration checks but is not a substitute for validating database-specific behavior against PostgreSQL.

## Code layout

- `api`: REST controller and exception mapping
- `api/dto`: request and response contracts
- `api/validation`: reusable request constraints
- `domain`: JPA entity and lifecycle callbacks
- `repository`: Spring Data persistence interface
- `service`: transactional application behavior
- `src/main/resources/db/migration`: append-only Flyway SQL
- `src/main/resources/static/openapi.yaml`: canonical Swagger/OpenAPI contract

## Change workflow

1. Add or update DTO validation and the OpenAPI contract with any public API change.
2. Add a new Flyway migration for schema changes; keep already-applied migrations immutable.
3. Add unit tests for service rules and integration tests for HTTP/database behavior.
4. Run `mvn -pl expense clean test` and review the migration against PostgreSQL syntax.
5. Update the relevant page under `docs/` and the quick-start README if behavior or setup changes.

## Current operational limitations

The app has no authentication or authorization. `paidBy` comes from the client and does not scope access. The API is not safe for untrusted network exposure until authentication and ownership checks are implemented. There is no React frontend, API gateway, or production deployment configuration yet.
