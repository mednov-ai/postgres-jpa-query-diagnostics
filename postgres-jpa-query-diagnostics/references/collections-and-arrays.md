# Collections and PostgreSQL Arrays

## Choose collection expansion or one SQL array

Use JPQL `IN` for a collection of scalar parameters:

```kotlin
@Query("select q from QuerySample q where q.id in :ids")
fun findByIdIn(@Param("ids") ids: Collection<UUID>): List<QuerySample>
```

Use PostgreSQL `ANY` when the query expects one array parameter:

```sql
external_id = ANY(?)
```

Do not use `setParameterList` for `ANY`; that API expands an IN-style collection rather than creating a PostgreSQL array.

## Bind arrays in a custom JDBC repository

```kotlin
fun findByExternalIds(ids: Array<UUID>?): List<QuerySampleRow> =
    dataSource.connection.use { connection ->
        connection.prepareStatement(
            "select * from query_sample where external_id = any(?)",
        ).use { statement ->
            if (ids == null) {
                statement.setNull(1, Types.ARRAY, "uuid[]")
            } else {
                statement.setArray(1, connection.createArrayOf("uuid", ids))
            }
            statement.executeQuery().use { resultSet -> mapRows(resultSet) }
        }
    }
```

Use `createArrayOf("text", values)` for `text[]`. Bind array-column assignments with the same typed null/value pattern.

## Define three-state behavior

Branch before SQL when the API semantics differ from raw PostgreSQL semantics:

```kotlin
fun search(ids: Array<UUID>?): List<QuerySampleRow> = when {
    ids == null -> findAll()       // null means no filter
    ids.isEmpty() -> emptyList()   // empty means match none
    else -> findByExternalIds(ids)
}
```

If raw SQL semantics are intended, remember:

- `value = ANY(NULL::uuid[])` is UNKNOWN.
- `value = ANY('{}'::uuid[])` is false.
- `value <> ALL('{}'::uuid[])` is true.

## Use array operators with typed arrays

Bind the right-hand value as a JDBC array for `&&`, `@>`, and `<@`. Bind the second argument of `array_position(uuid_array, ?)` as UUID, not bytes or text.

Do not serialize arrays to JSON, comma-separated text, or bytea merely to make binding compile.
