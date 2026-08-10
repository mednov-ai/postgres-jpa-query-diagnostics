# Performance

## Keep indexed columns bare

Prefer:

```sql
external_id = CAST(:value AS uuid)
```

Avoid by default:

```sql
CAST(external_id AS text) = :value
```

The first form can use an ordinary UUID index. The second expression requires a matching functional index or may scan the table.

## Prefer dynamic predicates for optional filters

When null means no filter, generate only the equality predicate for a non-null value. This produces simpler SQL than a large set of OR-null clauses and keeps type context close to the entity column.

Use OR-null only when a stable query shape is required and its generic/custom plan behavior is acceptable.

## Review semantic and plan changes together

Whenever changing OR-null, COALESCE, CASE, or casts:

- Include rows with SQL NULL in semantic tests.
- Compare exact result sets before comparing plans.
- Inspect index name, scan type, estimated/actual rows, and buffers.
- Do not optimize solely from elapsed time on a small fixture.
- Consider a functional index only when column transformation is part of the intended domain query, not a workaround for a wrong parameter type.

