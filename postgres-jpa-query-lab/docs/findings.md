# Результаты исследования

## Главный вывод

Одинаковое внешнее сообщение нельзя автоматически считать багом pgJDBC. В проверенном стеке ошибки разделились между Spring Data parameter parsing, HQL semantic validation, Hibernate argument validation, JDBC Java→SQL binding и тремя разными PostgreSQL-слоями: parser, operator/function resolution и assignment.

Минимальное исправление определяется местом сбоя:

1. До SQL — исправить HQL/Java type или binding имени.
2. PostgreSQL не знает тип NULL — дать тип параметру либо не генерировать предикат.
3. Оператор/функция не найдены — проверить фактический bind type и типы обоих операндов.
4. Assignment mismatch — исправить setter/PGobject/JDBC Array, не приводить индексированный столбец.

## Что подтвердилось

- `SELECT ? IS NULL` с `setNull(Types.NULL)` стабильно даёт 42P18; `CAST(? AS uuid)` и `setNull(Types.OTHER, "uuid")` работают.
- Hibernate выводит стандартные типы неравномерно. JPQL OR-null прошёл для UUID, Boolean и BigDecimal, но native enum и Instant дали нетипизированный первый SQL placeholder и 42P18.
- Исключение nullable-предиката через отдельный repository method сохранило семантику для null и обычного значения без Hibernate SPI.
- pgJDBC 42.7.11 не принимает `Instant` в обычном `setObject`; `OffsetDateTime` принимается. Hibernate при прямом сравнении entity attribute умеет bind Instant, но OR-null оставляет первый placeholder нетипизированным.
- `PGobject(type=jsonb)`, `Connection.createArrayOf`, typed `setNull` и UUID `setObject` подтверждены как корректные низкоуровневые исправления.
- Hibernate 6.6 автоматически маппит `Array<String>` и `Array<UUID>`; для native enum нужен `@JdbcTypeCode(SqlTypes.NAMED_ENUM)`, для JSONB — `SqlTypes.JSON`.

## Что было неправильным или недостаточным в исходных предположениях

- `text column but expression is of type bytea` не удалось воспроизвести на PostgreSQL 18.4: `setBytes` в text assignment был принят, а значение вернулось как bytea-текст. Поэтому рецепта для этой сигнатуры нет.
- PostgreSQL shorthand `:value::uuid` нельзя рекомендовать для Spring Data native query в этом стеке: Hibernate считает именем параметра `value::uuid`.
- COALESCE не является общей заменой OR-null: он изменил результат при NULL в столбце.
- Empty collection нельзя описывать единым правилом: JPQL/Hibernate и NamedParameterJdbcTemplate ведут себя по-разному.
- SQL CAST не помогает, если pgJDBC ещё до отправки SQL не умеет связать Java-объект, как в случае `Instant` через JdbcTemplate.
- Повтор одного named parameter в JPQL превращается в несколько JDBC placeholders; типовой контекст одного сравнения не гарантирует тип другого placeholder в `IS NULL`.

## Производительность

На таблице из 5000 строк после `ANALYZE`:

- `external_id = CAST(parameter AS uuid)` использовал `idx_plan_external_id`;
- `CAST(external_id AS text) = parameter` выполнил Seq Scan и не использовал обычный UUID index;
- OR-null с конкретным ненулевым литералом сохранил index-eligible план.

Абсолютное время не считается переносимым критерием. Рекомендация ограничена направлением приведения: приводить параметр, а не индексированный столбец, если это сохраняет тип и семантику.

## Не воспроизведено или не объявлено рецептом

- Точная assignment-ошибка `text ← bytea` на PostgreSQL 18.4.
- `Could not determine SqmExpressible` в текущих HQL-сценариях.
- Универсальная ошибка для JPQL null/empty collection: Hibernate 6.6 корректно переписал проверенные empty-list запросы.
- Querydsl: зависимость намеренно не добавлялась; Specifications/Criteria представлены архитектурным выводом об исключении nullable predicate, но отдельный Querydsl recipe отсутствует.
- Hibernate SPI для explicit BasicType не включён в рецепты: публичные SQL/JDBC/dynamic-query варианты оказались достаточными.

## Forward-тестирование скилла

Три свежих проверяющих агента получили только каталог скилла и семь новых задач: nullable native UUID, JPQL native enum, nullable UUID array, JPQL empty IN, nullable JSONB DML, повтор параметра в IS NULL/COALESCE/comparison и замаскированную UUID←bytea ошибку.

Все агенты:

- выбрали deepest meaningful exception и правильный первичный слой;
- предложили минимальные подтверждённые исправления и отделили hypotheses от recipes;
- предупредили об отличии null, empty и «не применять фильтр»;
- не рекомендовали Hibernate 5 API и не объявили ошибки багом pgJDBC;
- потребовали production-shaped тест для Kotlin List в ANY вместо необоснованного переноса SQLState 42809 с bytea-сценария.

Результаты соответствуют rubric, повторная редакция скилла не потребовалась.

## Воспроизведение

```bash
./postgres-jpa-query-lab/gradlew -p postgres-jpa-query-lab clean verifyDiagnostics
```

Диагностические записи со SQLState, root exception, bind API и проверенным результатом находятся в `postgres-jpa-query-lab/test-results/diagnostic-evidence.jsonl`.
