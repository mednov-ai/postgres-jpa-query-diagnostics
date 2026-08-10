# Error Signatures

Use the message to choose the next code inspection, not as the final diagnosis.

| Signature | Primary candidate | Inspect | Typical code direction |
|---|---|---|---|
| `could not determine data type of parameter $n`, 42P18 | PostgreSQL parser | SQL context of that exact placeholder | Cast the parameter or omit the predicate. |
| `column ... is of type ... but expression is of type bytea`, 42804 | PostgreSQL assignment | Setter and target column | Bind the domain/JDBC type instead of bytes. |
| `jsonb ... character varying`, 42804 | PostgreSQL assignment | `setString` or String repository parameter | Bind JSONB or cast the parameter in that mechanism. |
| `operator does not exist`, 42883 | PostgreSQL operator resolution | Concrete left/right operand types | Correct the parameter type; keep the column typed. |
| `function ... does not exist`, 42883 | PostgreSQL function resolution | Function signature and argument types | Bind the required element/value type. |
| `ANY/ALL ... requires array`, 42809 | PostgreSQL expression validation | Right-hand bind representation | Bind a real JDBC/PostgreSQL array. |
| `QueryArgumentException` | Hibernate type system | Entity attribute type versus runtime class | Change/convert the Java or Kotlin value. |
| `SemanticException` / SQM error | HQL parser | Compared expressions, CASE branches, HQL cast type | Rewrite the JPQL/HQL expression. |
| Named parameter not bound | Spring Data or Hibernate | Parsed parameter names and `@Param` values | Correct parameter syntax/binding. |
| `No value specified for parameter` | JDBC binding | Positional placeholder assignment | Set every parameter before execution. |
| `syntax error at or near "$n"`, 42601 | PostgreSQL parser | Parameter placed as identifier/keyword or empty `IN ()` | Compose whitelisted identifiers or branch empty input. |

Walk `cause` and `SQLException.nextException`. Prefer the deepest SQLState-bearing exception over an outer `BadSqlGrammarException` label.

