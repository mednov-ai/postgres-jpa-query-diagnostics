# JPQL and HQL Null Parameters

## Prefer typed equality queries

Keep non-null repository methods strongly connected to entity attributes:

```kotlin
@Query("select q from QuerySample q where q.status = :status")
fun findByStatus(@Param("status") status: QueryStatus): List<QuerySample>
```

When null means no filter, dispatch outside the query rather than forcing one nullable parameter through `IS NULL` and equality:

```kotlin
fun search(status: QueryStatus?): List<QuerySample> =
    status?.let(repository::findByStatus) ?: repository.findAll()
```

This is especially useful for native PostgreSQL enums and temporal values whose repeated placeholders may not receive identical JDBC types.

## Build optional filters with Specification

```kotlin
fun statusIs(status: QueryStatus?): Specification<QuerySample> =
    Specification { root, _, cb ->
        status?.let { cb.equal(root.get<QueryStatus>("status"), it) }
            ?: cb.conjunction()
    }
```

Compose only predicates represented by non-null filter values. Make empty-collection policy explicit before composing an `IN` predicate.

## Build optional filters with Criteria API

```kotlin
val cb = entityManager.criteriaBuilder
val query = cb.createQuery(QuerySample::class.java)
val root = query.from(QuerySample::class.java)
val predicates = mutableListOf<Predicate>()

filter.status?.let {
    predicates += cb.equal(root.get<QueryStatus>("status"), it)
}
filter.createdAfter?.let {
    predicates += cb.greaterThanOrEqualTo(root.get<Instant>("createdAt"), it)
}

query.where(*predicates.toTypedArray())
```

Use typed `root.get<T>()` calls where Kotlin inference is ambiguous.

## Handle JPQL collections at the API boundary

```kotlin
fun findByIds(ids: Collection<UUID>): List<QuerySample> = when {
    ids.isEmpty() -> emptyList()
    else -> repository.findByIdIn(ids)
}
```

Use a separate nullable API branch if null means no filter. Do not conflate null with empty.

## Keep HQL casts provider-specific

Use HQL cast names only after confirming the current Hibernate grammar and inferred type. Do not paste PostgreSQL `::type` syntax or assume PostgreSQL type names are valid HQL cast targets. Prefer typed predicates or dynamic query construction when a cast would couple JPQL to Hibernate internals.
