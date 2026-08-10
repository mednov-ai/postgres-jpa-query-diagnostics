# JDBC Binding

## Bind UUID

```kotlin
if (value == null) {
    statement.setNull(index, Types.OTHER, "uuid")
} else {
    statement.setObject(index, value)
}
```

Use `UUID`, not String or ByteArray, for non-null values.

## Bind JSONB

```kotlin
if (json == null) {
    statement.setNull(index, Types.OTHER, "jsonb")
} else {
    val value = PGobject().apply {
        type = "jsonb"
        this.value = objectMapper.writeValueAsString(json)
    }
    statement.setObject(index, value)
}
```

Decide whether null clears the column or skips the update before binding it.

## Bind PostgreSQL arrays

```kotlin
if (ids == null) {
    statement.setNull(index, Types.ARRAY, "uuid[]")
} else {
    val sqlArray = connection.createArrayOf("uuid", ids)
    statement.setArray(index, sqlArray)
}
```

Use element type `text` for `text[]`. Close the statement/result and release the JDBC Array when the driver/application lifecycle requires it.

## Supply SQL context for an unknown null

Use either a typed JDBC null or a parameter CAST owned by the SQL:

```sql
CAST(? AS uuid)
```

Do not bind an unknown null with `Types.NULL` in an expression such as `? IS NULL` and expect PostgreSQL to infer a type.

## Keep identifiers out of parameters

PreparedStatement parameters represent values, not table names, column names, operators, keywords, or type names. Build identifiers only from a strict allowlist:

```kotlin
val orderColumn = when (sort) {
    SortField.CREATED_AT -> "created_at"
    SortField.NAME -> "name"
}
val sql = "select * from query_sample order by $orderColumn"
```
