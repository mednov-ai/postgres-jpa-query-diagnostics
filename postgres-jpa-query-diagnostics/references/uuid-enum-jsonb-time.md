# UUID, Enum, JSONB, and Time Values

## Map UUID directly

Use `UUID`/`UUID?` in entities, repository methods, services, and JDBC bindings. Parse external text before calling the repository:

```kotlin
val externalId = request.externalId?.let(UUID::fromString)
repository.findByExternalId(externalId)
```

Do not change a repository UUID parameter to String to accommodate an API payload.

## Map a PostgreSQL native enum

```kotlin
enum class QueryStatus { NEW, PROCESSING, DONE }

@field:Enumerated(EnumType.STRING)
@field:JdbcTypeCode(SqlTypes.NAMED_ENUM)
@field:Column(columnDefinition = "querystatus")
var status: QueryStatus
```

Hibernate derives the PostgreSQL named-enum type from the enum class name. Align the Flyway type name or explicitly prove any customized mapping. Do not assume `@Enumerated(EnumType.STRING)` alone binds a native enum; by itself it represents character storage.

Use an `AttributeConverter` only when the column stores an application code rather than a PostgreSQL native enum:

```kotlin
@Converter
class QueryStatusConverter : AttributeConverter<QueryStatus, String> {
    override fun convertToDatabaseColumn(value: QueryStatus?): String? = value?.name
    override fun convertToEntityAttribute(value: String?): QueryStatus? =
        value?.let(QueryStatus::valueOf)
}
```

## Map JSONB through Hibernate

```kotlin
@field:JdbcTypeCode(SqlTypes.JSON)
@field:Column(columnDefinition = "jsonb")
var metadata: JsonNode? = null
```

Keep a JSON mapper such as Jackson on the classpath. For low-level DML, bind JSONB using the JDBC pattern in [jdbc-binding.md](jdbc-binding.md). Do not assume a JSON String is automatically a JSONB JDBC value.

## Map PostgreSQL arrays

Hibernate 6 can map basic object arrays directly:

```kotlin
@field:Column(columnDefinition = "text[]")
var tags: Array<String>? = null

@field:Column(name = "related_ids", columnDefinition = "uuid[]")
var relatedIds: Array<UUID>? = null
```

Avoid data-class entity equality for array properties because JVM array equality is referential.

## Bind time deliberately

- Use `LocalDate` for PostgreSQL `date`.
- Use `OffsetDateTime` for direct pgJDBC `timestamptz` binding.
- Use `Instant` naturally on Hibernate entity attributes, but do not assume direct `PreparedStatement.setObject(Instant)` is supported.
- Convert direct JDBC `Instant` values to `OffsetDateTime` or another explicitly supported JDBC representation before binding.

