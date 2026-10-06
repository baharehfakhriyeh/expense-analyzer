# Database model

## Expense table

The `Expense` entity maps to PostgreSQL table `expenses`.

| Column | Type | Nullability / constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` identity | Primary key | Generated expense identifier |
| `description` | `VARCHAR(200)` | Required | Expense description |
| `amount` | `NUMERIC(19,4)` | Required, greater than zero | Monetary amount |
| `currency` | `VARCHAR(3)` | Required | Uppercase ISO 4217 code at application level |
| `expense_date` | `DATE` | Required | Date the expense occurred |
| `category` | `VARCHAR(80)` | Required | Expense category |
| `notes` | `VARCHAR(2000)` | Optional | Additional details |
| `paid_by` | `BIGINT` | Required, greater than zero | External/person identifier; deliberately no foreign key |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | Required, immutable in the entity | Creation timestamp |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | Required | Last update timestamp |
| `version` | `BIGINT` | Required, default `0` | JPA optimistic-lock version |

Java validates ISO currency membership and the application validates request lengths and precision. The database migration enforces storage lengths, positive amounts, and positive `paid_by` values.

## Indexes

The current schema indexes `expense_date` and `paid_by`. The category index was removed in Flyway migration `V2__drop_category_index.sql`; category filtering remains available but may use a sequential scan. Add an index only if production query plans and workload measurements justify its write and storage cost. Because category matching applies `lower(category)`, a future index for that predicate would need to match the expression.

## Schema lifecycle

Flyway migrations are append-only and versioned in `expense/src/main/resources/db/migration`:

- `V1__create_expenses.sql` creates the table and initial indexes.
- `V2__drop_category_index.sql` drops the category index without rewriting the applied V1 migration.

Do not edit a migration that has been applied to a shared database. Add a new migration for subsequent schema changes. Hibernate uses `ddl-auto: validate`; Flyway, not Hibernate schema generation, changes the runtime schema.

## Ownership and future authentication integration

`paid_by` is a scalar `long`, not an entity relation or foreign key. This keeps the expense module independent of the future person/authentication module, but it also means the database cannot validate that a referenced person exists. When authentication is integrated, derive the paid-by identity from the authenticated principal rather than trusting a caller-provided ID, and decide whether a foreign key is appropriate for the deployment architecture.
