# Native Null Parameters

## Write parameter-side casts

Use standard SQL CAST in Spring Data native queries:

```kotlin
@Query(
    value = """
        select *
        from query_sample q
        where (cast(:externalId as uuid) is null
               or q.external_id = cast(:externalId as uuid))
    """,
    nativeQuery = true,
)
fun findByExternalId(@Param("externalId") externalId: UUID?): List<QuerySample>
```

Cast every occurrence that may become an independent placeholder. Keep the indexed column unmodified.

Do not write `:externalId::uuid` in a Spring Data native named query. Hibernate may parse `externalId::uuid` as the parameter name.

## Omit optional predicates when appropriate

When null means "do not filter," prefer explicit method dispatch over a complex nullable predicate:

```kotlin
fun findSamples(externalId: UUID?): List<QuerySample> =
    if (externalId == null) repository.findAll()
    else repository.findByExternalId(externalId)
```

Use this shape when it improves semantics or avoids repeated untyped placeholders. Keep both repository methods strongly typed.

## Keep COALESCE semantics explicit

Do not treat these predicates as equivalent:

```sql
:value IS NULL OR column_name = :value
column_name = COALESCE(:value, column_name)
```

The second predicate excludes rows whose `column_name` is SQL NULL when `:value` is null. Use it only if that exclusion is intended.

## Split conflicting occurrences

If one logical value is used in incompatible contexts, use separate typed parameters or separate query branches:

```sql
CASE
  WHEN CAST(:filterValue AS uuid) IS NULL THEN external_id
  ELSE CAST(:replacementValue AS uuid)
END
```

Do not rely on a comparison occurrence to type an unrelated `IS NULL`, CASE, COALESCE, or function occurrence.

