# Hibernate Binding

## Fix Java type mismatches before SQL

If Hibernate expects UUID, enum, or time and rejects the argument, convert the value before `setParameter`:

```kotlin
val id: UUID = UUID.fromString(rawId)
entityManager.createQuery(
    "select q from QuerySample q where q.externalId = :id",
    QuerySample::class.java,
).setParameter("id", id)
```

Do not add a PostgreSQL cast when Hibernate has already rejected the Java argument.

## Use Hibernate 6 typed parameter APIs only when needed

```kotlin
val query = entityManager.createQuery(jpql)
    .unwrap(org.hibernate.query.Query::class.java)

query.setParameter("id", id, UUID::class.java)
query.setParameter("id", id, StandardBasicTypes.UUID)
query.setParameterList("ids", ids, UUID::class.java)
```

Prefer entity-attribute inference and strongly typed repository methods. Use explicit Hibernate types only when the query lacks a reliable attribute context, and keep the provider dependency visible in the code boundary.

## Distinguish collection APIs

- Use `setParameterList` for JPQL/HQL `IN (:ids)` expansion.
- Use one `setParameter` only if a native query is intentionally binding one mapped SQL array.
- Prefer `Connection.createArrayOf` in custom JDBC code when Hibernate array typing is unclear.

## Inspect generated placeholders

Enable SQL and binding logs during diagnosis:

```yaml
logging:
  level:
    org.hibernate.SQL: DEBUG
    org.hibernate.orm.jdbc.bind: TRACE
```

Map each named occurrence to its positional placeholder. A repeated parameter in `IS NULL`, CASE, COALESCE, and equality can receive different SQL context even when the Java value is shared.

Do not use Hibernate 5 `Type` examples or copy internal SPI code without a version-specific need.

