# Матрица интеграционных тестов

Дата последнего полного прогона: 2026-08-10. Команда: `./gradlew clean verifyDiagnostics`.

## Проверенный стек

| Компонент | Версия | Источник версии |
|---|---:|---|
| Java | 21 | Gradle toolchain |
| Kotlin | 1.9.25 | Spring Boot dependency management |
| Spring Boot | 3.5.16 | runtime assertion |
| Spring Data JPA | 3.5.13 | Spring Boot BOM |
| Hibernate ORM | 6.6.53.Final | `org.hibernate.Version` assertion |
| pgJDBC | 42.7.11 | JDBC metadata assertion |
| Flyway | 11.7.2 | runtime jar assertion |
| Testcontainers | 1.21.4 | Spring Boot BOM |
| JUnit Jupiter | 5.12.2 | Spring Boot BOM |
| PostgreSQL | 18.4 | `SHOW server_version` assertion |

Hibernate и pgJDBC не переопределены. PostgreSQL запускается из образа `postgres:18.4`.

## Выполненные группы

| Группа | Тестов | Механизмы | Основные проверки |
|---|---:|---|---|
| FixtureAndVersionsIT | 2 | JdbcTemplate | Версии, SQL NULL, пустая строка/JSON/массив, непустые значения |
| NativeScalarMatrixIT | 12 | JdbcTemplate, NamedParameterJdbcTemplate | null/valid для text, UUID, Boolean, Int, Long, numeric, date, timestamptz, enum, JSONB; Instant binding |
| JdbcParameterInferenceIT | 8 | PreparedStatement | 42P18, 42883, 42601, незаполненный positional parameter, CAST и typed setNull |
| NativeDmlBindingIT | 5 | PreparedStatement | UPDATE/RETURNING, UUID←bytea, JSONB←varchar, фактическое поведение text←bytea |
| CollectionsAndArraysIT | 12 | PreparedStatement | null/empty/one/many UUID[], ANY, ALL, array_position, `&&`, `@>`, `<@`, assignment |
| NativeCollectionExpansionIT | 3 | NamedParameterJdbcTemplate | IN с one/many, пустой List и short-circuit fix |
| JpqlAndNativeQueryIT | 12 | Spring Data JPQL/native, EntityManager JPQL/native | nullable UUID/enum/time, CAST против `::`, COALESCE, empty IN, QueryArgumentException |
| EnumConverterAndErrorCatalogIT | 4 | Spring Data JPQL, EntityManager | native enum, AttributeConverter, enum collection, unbound parameter, SemanticException |
| DynamicPredicateAlternativesIT | 2 | Specification, Criteria API | исключение nullable enum predicate и совпадение семантики |
| PredicatePlanIT | 2 | JdbcTemplate | EXPLAIN ANALYZE BUFFERS, cast параметра против cast столбца, OR-null и COALESCE |
| SkillRecipesContractTest | 1 | filesystem contract | каждый recipe имеет версии и реальные `reproduces_`/`fixes_` методы |

Итого: 63 зелёных теста, из них 62 работают с реальным PostgreSQL 18.4 и один проверяет доказательную связь скилла.

## Подтверждённые ошибки и исправления

| Сигнатура | Слой | Воспроизведение | Проверенное исправление |
|---|---|---|---|
| `42P18 could not determine data type` | PostgreSQL parser | `SELECT ? IS NULL`, JPQL nullable native enum/Instant | CAST параметра, typed `setNull`, либо исключение nullable-предиката |
| `42804 uuid ... bytea` | PostgreSQL assignment | UUID/UUID[] DML с `setBytes` | UUID `setObject`, typed `setNull`; array `createArrayOf` |
| `42804 jsonb ... varchar` | PostgreSQL assignment | JSONB DML с `setString` | `PGobject(jsonb)`, typed null либо CAST параметра |
| `42883 operator does not exist` | PostgreSQL operator resolution | `uuid = bytea` | UUID bind вместо bytea |
| `42883 function ... does not exist` | PostgreSQL function resolution | `array_position(uuid[], bytea)` | UUID element bind |
| `42809 ANY requires array` | PostgreSQL expression validation | bytea справа от ANY | JDBC Array через `createArrayOf` |
| `42601 syntax error near $1` | PostgreSQL parser | параметр вместо идентификатора; empty expanded IN | не параметризовать идентификаторы; short-circuit empty collection |
| `QueryArgumentException` | Hibernate validation | String для UUID JPQL-параметра | передать UUID/совпадающий Java type |
| Unknown named parameter | Spring Data/Hibernate binding | `:value::uuid` распознан как `value::uuid` | `CAST(:value AS uuid)` |
| `SemanticException` | HQL semantic parser | сравнение UUID-атрибута со String-атрибутом | согласовать типы до SQL |

## Семантика коллекций и nullable-фильтров

- `empty uuid[]` с `= ANY` возвращает 0 строк; `<> ALL(empty)` истинно для всех четырёх seed-строк, включая строку с NULL expression.
- NULL PostgreSQL array даёт UNKNOWN и не эквивалентен empty array или «не применять фильтр».
- JPQL `IN emptyList()` в Hibernate 6.6 возвращает пустой результат, а NamedParameterJdbcTemplate разворачивает empty List в недопустимый `IN ()`.
- `column = COALESCE(NULL, column)` исключает строки, где column равен SQL NULL: в fixture это 3 строки против 4 для `NULL IS NULL OR ...`.
- Native `:value::uuid` конфликтует с Hibernate named-parameter parser; стандартный `CAST(:value AS uuid)` работает для null и UUID.
