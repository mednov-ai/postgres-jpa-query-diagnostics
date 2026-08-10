# Verified Recipes

Apply these recipes only to the recorded mechanism and baseline. Re-test other versions or providers.

## Unknown null in an untyped JDBC expression

### Symptom

Observe `could not determine data type of parameter $1`, SQLState 42P18, from `select ? is null` with `setNull(Types.NULL)`.

### Failing query

Use `select ? is null` with an untyped SQL NULL.

### Root cause

Assign the failure to the PostgreSQL parser: the placeholder has no SQL type context.

### Verified fix

Use `select cast(? as uuid) is null`, or bind the same expression with `setNull(1, Types.OTHER, "uuid")` when UUID is the intended type.

### Alternatives

Prefer the SQL cast when the query owns the expected type. Prefer typed `setNull` when the binding layer owns it.

### Do not use

Do not cast an indexed column to type an unrelated placeholder.

### Verified with

- Spring Boot: 3.5.16
- Hibernate: 6.6.53.Final
- pgJDBC: 42.7.11
- PostgreSQL: 18.4
- Tests: `JdbcParameterInferenceIT.reproduces_unknown_null_parameter_42p18`, `JdbcParameterInferenceIT.fixes_unknown_null_parameter_with_sql_cast`, `JdbcParameterInferenceIT.fixes_unknown_null_parameter_with_typed_set_null`

## Nullable PostgreSQL native enum in JPQL

### Symptom

Observe 42P18 from `(:value is null or q.status = :value)` when `value` is null.

### Failing query

Use the Spring Data JPQL native-enum predicate recorded as `jpql-nullable-native-enum-42p18`.

### Root cause

Assign the failure to the PostgreSQL parser: the first generated placeholder remains untyped despite the comparison occurrence.

### Verified fix

Omit the predicate for null by selecting `findAll()`; execute typed `q.status = :value` for a non-null enum.

### Alternatives

Add a separate integration test before using Specification or Criteria for the same branching behavior.

### Do not use

Do not assume entity enum metadata types every generated placeholder.

### Verified with

- Spring Boot: 3.5.16
- Hibernate: 6.6.53.Final
- pgJDBC: 42.7.11
- PostgreSQL: 18.4
- Tests: `JpqlAndNativeQueryIT.reproduces_jpql_nullable_native_enum_42p18`, `JpqlAndNativeQueryIT.fixes_jpql_nullable_native_enum_with_dynamic_predicate`

## Hibernate JPQL UUID argument mismatch

### Symptom

Observe `QueryArgumentException` before SQL execution when binding a String to a UUID parameter.

### Failing query

Use `where q.externalId = :value` and bind `"not-a-uuid"` as String.

### Root cause

Assign the failure to Hibernate parameter validation, not PostgreSQL.

### Verified fix

Change the runtime/method parameter to `UUID` and bind the UUID value.

### Alternatives

Parse and validate external text before query binding.

### Do not use

Do not add a PostgreSQL cast to a query that never reaches JDBC.

### Verified with

- Spring Boot: 3.5.16
- Hibernate: 6.6.53.Final
- pgJDBC: 42.7.11
- PostgreSQL: 18.4
- Tests: `JpqlAndNativeQueryIT.reproduces_hibernate_parameter_value_type_validation`, `JpqlAndNativeQueryIT.fixes_hibernate_parameter_with_matching_java_type`

## UUID compared with bytea

### Symptom

Observe `operator does not exist: uuid = bytea`, SQLState 42883.

### Failing query

Bind `ByteArray` with `setBytes` to `external_id = ?`.

### Root cause

Assign the server failure to PostgreSQL operator resolution and trace the bytea operand to JDBC binding.

### Verified fix

Bind a `UUID` with `setObject`; verify the matching row.

### Alternatives

Validate and convert text input to UUID before binding.

### Do not use

Do not cast the UUID column to bytea or text merely to accept a wrong Java type.

### Verified with

- Spring Boot: 3.5.16
- Hibernate: 6.6.53.Final
- pgJDBC: 42.7.11
- PostgreSQL: 18.4
- Tests: `JdbcParameterInferenceIT.reproduces_operator_resolution_42883`, `JdbcParameterInferenceIT.fixes_operator_resolution_with_uuid_binding`

## UUID column assignment from bytea

### Symptom

Observe a UUID-column/bytea assignment mismatch, SQLState 42804.

### Failing query

Bind `ByteArray` with `setBytes` to `update ... set external_id = ?`.

### Root cause

Assign the server failure to PostgreSQL assignment and trace the expression type to JDBC binding.

### Verified fix

Bind null with `setNull(Types.OTHER, "uuid")`; bind a value with `setObject(UUID)`. Verify both RETURNING results.

### Alternatives

Use a parameter cast only after adding an equivalent null-and-value DML test for the wrapper API.

### Do not use

Do not use `setBytes` for UUID values.

### Verified with

- Spring Boot: 3.5.16
- Hibernate: 6.6.53.Final
- pgJDBC: 42.7.11
- PostgreSQL: 18.4
- Tests: `NativeDmlBindingIT.reproduces_uuid_assignment_from_bytea_42804`, `NativeDmlBindingIT.fixes_uuid_assignment_with_typed_null_and_value`

## JSONB column assignment from varchar

### Symptom

Observe a jsonb-column/varchar assignment mismatch, SQLState 42804.

### Failing query

Bind JSON text with `setString` to `update ... set metadata = ?`.

### Root cause

Assign the server failure to PostgreSQL assignment: pgJDBC supplies varchar, not jsonb.

### Verified fix

Bind null with `setNull(Types.OTHER, "jsonb")`; bind a `PGobject` whose type is `jsonb`. Verify both RETURNING results.

### Alternatives

Add a test before substituting a SQL cast or Hibernate JSON type.

### Do not use

Do not treat syntactically valid JSON in a String as a typed jsonb value.

### Verified with

- Spring Boot: 3.5.16
- Hibernate: 6.6.53.Final
- pgJDBC: 42.7.11
- PostgreSQL: 18.4
- Tests: `NativeDmlBindingIT.reproduces_jsonb_assignment_from_varchar_42804`, `NativeDmlBindingIT.fixes_jsonb_assignment_with_pgobject`

## UUID array supplied as a non-array

### Symptom

Observe `op ANY/ALL (array) requires array on right side`, SQLState 42809.

### Failing query

Bind bytea with `setBytes` to `external_id = ANY(?)`.

### Root cause

Assign the server failure to PostgreSQL operator resolution and trace the non-array operand to JDBC binding.

### Verified fix

Create a JDBC array with `connection.createArrayOf("uuid", values)` and bind it with `setArray`.

### Alternatives

Use an expanded `IN` collection only after testing its empty/null semantics separately.

### Do not use

Do not pass `ByteArray`, Kotlin Array, or List as though it were automatically a PostgreSQL array.

### Verified with

- Spring Boot: 3.5.16
- Hibernate: 6.6.53.Final
- pgJDBC: 42.7.11
- PostgreSQL: 18.4
- Tests: `CollectionsAndArraysIT.reproduces_any_with_non_array_42809`, `CollectionsAndArraysIT.fixes_any_with_create_array_of`

## UUID array column assignment from bytea

### Symptom

Observe a uuid-array/bytea assignment mismatch, SQLState 42804.

### Failing query

Bind bytea with `setBytes` to `update ... set related_ids = ?`.

### Root cause

Assign the server failure to PostgreSQL assignment and trace bytea to JDBC binding.

### Verified fix

Bind null with `setNull(Types.ARRAY, "uuid[]")`; bind a value with `setArray(createArrayOf("uuid", values))`. Verify null and exact-array RETURNING values.

### Alternatives

Add a separate test before using a provider-specific array type.

### Do not use

Do not serialize a UUID array to bytes.

### Verified with

- Spring Boot: 3.5.16
- Hibernate: 6.6.53.Final
- pgJDBC: 42.7.11
- PostgreSQL: 18.4
- Tests: `CollectionsAndArraysIT.reproduces_uuid_array_assignment_from_bytea_42804`, `CollectionsAndArraysIT.fixes_uuid_array_assignment_with_typed_null_and_create_array_of`

## UUID array_position element mismatch

### Symptom

Observe `function array_position(uuid[], bytea) does not exist`, SQLState 42883.

### Failing query

Bind bytea with `setBytes` as the second argument to `array_position(related_ids, ?)`.

### Root cause

Assign the failure to PostgreSQL function resolution and trace the bytea argument to JDBC binding.

### Verified fix

Bind a UUID with `setObject(UUID)` and verify the containing row.

### Alternatives

Convert validated input to UUID before binding.

### Do not use

Do not cast the UUID-array column to accommodate a bytea element.

### Verified with

- Spring Boot: 3.5.16
- Hibernate: 6.6.53.Final
- pgJDBC: 42.7.11
- PostgreSQL: 18.4
- Tests: `CollectionsAndArraysIT.reproduces_array_position_with_wrong_element_type_42883`, `CollectionsAndArraysIT.fixes_array_position_with_uuid_element`
