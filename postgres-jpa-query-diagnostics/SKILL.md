---
name: postgres-jpa-query-diagnostics
description: Diagnose and fix PostgreSQL, pgJDBC, Hibernate, Spring Data JPA, JPQL/HQL, and native SQL parameter failures involving nulls, unknown types, UUID, enum, JSONB, temporal values, collections, or PostgreSQL arrays. Use when writing or repairing repository queries, Criteria/Specification predicates, Hibernate parameter binding, JDBC setters, native DML, or query tests after SQLState 42P18, 42804, 42883, bytea/varchar mismatches, SQM errors, or surprising null/empty behavior.
---

# PostgreSQL JPA Query Diagnostics

## Work from code to failure layer

1. Inspect the repository method, JPQL/native query, entity mapping, Kotlin parameter type, and caller semantics.
2. Read the deepest meaningful exception, including `SQLException.nextException`.
3. Classify one primary layer before editing code:

| Failure boundary | Primary layer |
|---|---|
| Repository signature or named parameter rejected | Spring Data |
| HQL/SQM grammar or incompatible expressions | JPQL/HQL parser |
| Java argument rejected before JDBC | Hibernate type system |
| Java value cannot be bound or a placeholder is unset | JDBC binding |
| Untyped `$n` or parameter in illegal SQL position | PostgreSQL parser |
| Operator/function missing for resolved types | PostgreSQL operator resolution |
| Expression cannot be stored in target column | PostgreSQL assignment |

4. Inspect every generated JDBC placeholder independently. Repeated named parameters may become multiple placeholders with different type contexts.
5. State the intended behavior for null, empty, and non-empty input before selecting a code change.

## Choose the implementation shape

Use the narrowest code change that fixes the classified layer:

- Change the Kotlin/Java parameter type when Hibernate rejects the runtime argument.
- Use a typed entity comparison in JPQL when entity metadata can supply the type.
- Omit a predicate with Specification, Criteria, or method dispatch when null means "do not filter."
- Apply standard SQL `CAST(:value AS type)` to a native parameter when PostgreSQL lacks type context.
- Bind PostgreSQL-specific JDBC values with typed `setNull`, `PGobject`, or `Connection.createArrayOf`.
- Split a parameter or query when the same logical value is used in incompatible contexts.

Prefer parameter-side fixes:

```sql
indexed_uuid_column = CAST(:value AS uuid)
```

Avoid column-side coercion unless a matching functional index is intentional:

```sql
CAST(indexed_uuid_column AS text) = :value
```

## Preserve semantics

Treat these states as distinct API inputs:

- Scalar null: ignore filter, compare to SQL NULL, clear a column, leave a column unchanged, or reject.
- Null collection/array: ignore filter, match nothing, or reject.
- Empty collection: an expanded `IN` value set with provider-specific behavior.
- Empty PostgreSQL array: `= ANY(empty)` is false; `<> ALL(empty)` is true.
- Non-empty collection/array: match using the declared element type.

Do not replace an OR-null predicate with COALESCE without deciding how SQL NULL column values must behave. `column = COALESCE(NULL, column)` excludes rows where `column` is SQL NULL.

## Apply code patterns

Read only the reference matching the implementation boundary:

| Code being written | Read |
|---|---|
| Spring Data native nullable scalar, CAST, COALESCE | [native-null-parameters.md](references/native-null-parameters.md) |
| JPQL, repository branching, Specification, Criteria | [jpql-null-parameters.md](references/jpql-null-parameters.md) |
| `IN`, `ANY`, `ALL`, PostgreSQL arrays | [collections-and-arrays.md](references/collections-and-arrays.md) |
| UUID, enum mapping, JSONB, temporal values | [uuid-enum-jsonb-time.md](references/uuid-enum-jsonb-time.md) |
| Hibernate parameter APIs and generated placeholders | [hibernate-binding.md](references/hibernate-binding.md) |
| PreparedStatement setters and PostgreSQL-specific values | [jdbc-binding.md](references/jdbc-binding.md) |
| SQLState and exception classification | [error-signatures.md](references/error-signatures.md) |
| Index-safe predicate structure | [performance.md](references/performance.md) |
| Full diagnostic sequence | [diagnostic-workflow.md](references/diagnostic-workflow.md) |
| Provenance and exact verified scope | [verified-recipes.md](references/verified-recipes.md) |

## Avoid unverified shortcuts

- Do not use `:value::uuid` in a Spring Data native named query; use `CAST(:value AS uuid)`.
- Do not add PostgreSQL casts to fix a Hibernate error that occurs before SQL execution.
- Do not bind UUID or UUID-array values with `setBytes`.
- Do not assign JSONB with `setString` unless the SQL parameter is explicitly cast and that exact repository path is tested.
- Do not pass a Kotlin List to `ANY` as though it were a PostgreSQL array.
- Do not use Hibernate 5 type APIs or unverified provider SPI.
- Do not describe a workaround as verified unless it appears in [verified-recipes.md](references/verified-recipes.md).

## Verify changed code

Add a reproduction test for the original layer and a result-oriented test for the new code. Cover null and ordinary values; add empty and non-empty cases for collections. Assert the meaningful exception and SQLState for server failures, then assert exact rows or persisted values for the fix. When the indexed expression changes, inspect the execution plan.

Use `verified-recipes.md` only to confirm applicability and provenance; keep implementation guidance in the focused references above.
