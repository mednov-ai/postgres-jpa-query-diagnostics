# Diagnostic Workflow

## Inspect the affected code

Locate the complete path from API input to SQL:

1. Read the controller/service input type and its null/empty contract.
2. Read the repository method signature and `@Query` declaration.
3. Read the entity attribute mapping and PostgreSQL column type.
4. Identify whether the query is JPQL/HQL or native SQL.
5. Inspect caller-side conversions and any custom repository/JDBC code.
6. Capture generated SQL and parameter binding when the source query does not reveal the wire type.

Record the declared Kotlin type, runtime value class, expected PostgreSQL type, and actual bind API. Inspect repeated named parameters as separate generated placeholders.

## Stop at the first failing boundary

- Fix repository parameter names and method validation at Spring Data.
- Fix incompatible expressions and casts at HQL/SQM parsing.
- Fix runtime Java types at Hibernate argument validation.
- Fix setters and PostgreSQL-specific objects at JDBC binding.
- Fix missing parameter context with a parameter CAST at PostgreSQL parsing.
- Fix operand or function argument types for 42883.
- Fix DML value types for 42804.

Do not edit a lower layer if execution never reached it. A PostgreSQL cast cannot repair `QueryArgumentException`; changing pgJDBC cannot repair an invalid HQL expression.

## Select the code owner

| Problem | Preferred owner |
|---|---|
| External text must become UUID/enum/date | Service/input mapper |
| Null means omit a filter | Specification, Criteria, or service repository dispatch |
| Native placeholder lacks SQL context | Native SQL parameter CAST |
| PostgreSQL JSONB/array DML | Custom JDBC repository or tested native query |
| Same parameter has conflicting contexts | Query structure or separate parameters |
| Indexed column is being cast | Query predicate; move cast to parameter |

Keep controllers free of JDBC and persistence-specific conversion logic. Put dynamic predicate construction in repository specifications/custom repositories and input conversion in services or mappers.

## Write the change

Make one primary correction at the classified layer. Preserve the public null/empty contract. Avoid mixing a type correction with an unrelated query rewrite.

When no reusable recipe matches, implement the smallest production-shaped experiment first and keep the proposed code explicitly provisional until its result and semantics are established.

