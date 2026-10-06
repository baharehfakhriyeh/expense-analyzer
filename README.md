# Expense Analyzer

Expense Analyzer is a Java 21, Spring Boot REST API for recording expenses. The Maven root is an aggregator project; the runnable `expense` module contains the API, persistence layer, Flyway migrations, and Swagger UI.

Authentication is not implemented. `paidBy` is currently a client-supplied positive numeric identifier and is not checked against a person table. Do not expose this application to an untrusted network until authentication and authorization are added.

## Quick start

Requirements: JDK 21, Maven 3.6.3 or later, and Docker Compose.

Start PostgreSQL and the API:

```shell
docker compose up -d postgres
mvn -pl expense spring-boot:run
```

The committed default API port is `8087`. Open Swagger UI at [`http://localhost:8080/swagger-ui.html`](http://localhost:8080/swagger-ui.html). The canonical OpenAPI contract is served at [`http://localhost:8080/openapi.yaml`](http://localhost:8080/openapi.yaml); springdoc also exposes generated API metadata at `/v3/api-docs`.

The port and database connection can be configured with `SERVER_PORT`, `DATABASE_URL`, `DATABASE_USERNAME`, and `DATABASE_PASSWORD`. The application applies Flyway migrations at startup and validates the resulting schema through Hibernate.

Build, package, and test:

```shell
mvn -pl expense clean test
mvn -pl expense package
java -jar expense/target/expense-0.1.0-SNAPSHOT.jar
```

Integration tests use H2 and execute the Flyway migrations; they do not require Docker.

## Documentation

- [Architecture and request flow](docs/architecture.md)
- [REST API guide](docs/api.md)
- [Database model and migrations](docs/database.md)
- [Development and operations](docs/development.md)
- [Complete OpenAPI 3.0.3 specification](expense/src/main/resources/static/openapi.yaml)

## Project layout

```text
expense-analyzer/
├── pom.xml                         # Maven parent/aggregator; Java 21 and shared versions
├── docs/                           # Architecture, API, database, and development guides
└── expense/                        # Runnable Spring Boot REST API module
    ├── src/main/java/              # API, DTOs, service, repository, and Expense entity
    ├── src/main/resources/         # Application config, Flyway migrations, OpenAPI contract
    └── src/test/                   # Unit and H2-backed integration tests
```
