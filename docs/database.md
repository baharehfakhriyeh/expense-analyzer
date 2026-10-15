# Database model

## Expense table

The `Expense` entity maps to PostgreSQL table `expenses`.

| Column | Type | Nullability / constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` identity | Primary key | Generated expense identifier |
| `description` | `VARCHAR(200)` | Optional | Expense description |
| `amount` | `NUMERIC(19,4)` | Required, greater than zero | Monetary amount |
| `currency` | `VARCHAR(3)` | Required | Uppercase ISO 4217 code at application level |
| `expense_date` | `DATE` | Required | Date the expense occurred |
| `category_id` | `BIGINT` | Required, foreign key to `categories.id` | Category assigned to the expense |
| `notes` | `VARCHAR(2000)` | Optional | Additional details |
| `paid_by` | `BIGINT` | Required, greater than zero | External/person identifier; deliberately no foreign key |
| `created_at` | `TIMESTAMP WITH TIME ZONE` | Required, immutable in the entity | Creation timestamp |
| `updated_at` | `TIMESTAMP WITH TIME ZONE` | Required | Last update timestamp |
| `version` | `BIGINT` | Required, default `0` | JPA optimistic-lock version |

Java validates ISO currency membership and the application validates request lengths and precision. The database migration enforces storage lengths, positive amounts, positive `paid_by` values, and the category foreign key.

## Category table

The `Category` entity maps to PostgreSQL table `categories`.

| Column | Type | Nullability / constraints | Description |
|---|---|---|---|
| `id` | `BIGINT` identity | Primary key | Generated category identifier |
| `name` | `VARCHAR(80)` | Required, unique, lowercase, and trimmed | Category name |

The service trims category names and converts them to lowercase before saving. A database check constraint also rejects names that are not already trimmed and lowercase. The unique constraint therefore prevents duplicate names after normalization.

Each expense has one required category; a category can be referenced by many expenses. The foreign key prevents deleting a category while expenses reference it.

## Indexes

The current schema indexes `expense_date`, `paid_by`, and `category_id`. The category ID index supports expense filtering by category. The former text category index was removed in Flyway migration `V2__drop_category_index.sql` before category names moved into their own table.

## Schema lifecycle

Flyway migrations are append-only and versioned in `expense/src/main/resources/db/migration`:

- `V1__create_expenses.sql` creates the table and initial indexes.
- `V2__drop_category_index.sql` drops the category index without rewriting the applied V1 migration.
- `V3__allow_null_expense_description.sql` allows descriptions to be null.
- `V4__add_categories.sql` creates `categories`, replaces the expense category text column with a required foreign key, and adds the category ID index and name constraint. It assumes the expense table is empty when applied because existing category text values are not migrated.

Do not edit a migration that has been applied to a shared database. Add a new migration for subsequent schema changes. Hibernate uses `ddl-auto: validate`; Flyway, not Hibernate schema generation, changes the runtime schema.

## Ownership and future authentication integration

`paid_by` is a scalar `long`, not an entity relation or foreign key. This keeps the expense module independent of the future person/authentication module, but it also means the database cannot validate that a referenced person exists. When authentication is integrated, derive the paid-by identity from the authenticated principal rather than trusting a caller-provided ID, and decide whether a foreign key is appropriate for the deployment architecture.
