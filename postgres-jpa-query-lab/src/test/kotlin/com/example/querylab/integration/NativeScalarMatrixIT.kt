package com.example.querylab.integration

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID
import java.util.stream.Stream

class NativeScalarMatrixIT : AbstractPostgresIntegrationTest() {
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Autowired
    lateinit var namedJdbcTemplate: NamedParameterJdbcTemplate

    @ParameterizedTest(name = "CAST nullable scalar {0} as {1}")
    @MethodSource("scalarCases")
    fun cast_supplies_type_context_for_null_and_valid_values(label: String, sqlType: String, value: Any) {
        val sql = "select cast(? as $sqlType) is null"
        assertThat(jdbcTemplate.queryForObject(sql, Boolean::class.java, null)).isTrue
        assertThat(jdbcTemplate.queryForObject(sql, Boolean::class.java, value)).isFalse

        val namedSql = "select cast(:value as $sqlType) is null"
        assertThat(
            namedJdbcTemplate.queryForObject(
                namedSql,
                MapSqlParameterSource("value", null),
                Boolean::class.java,
            ),
        ).`as`(label).isTrue
    }

    @Test
    fun reproduces_pgjdbc_cannot_infer_java_time_instant() {
        val thrown = catchThrowable {
            jdbcTemplate.queryForObject(
                "select cast(? as timestamptz) is null",
                Boolean::class.java,
                Instant.parse("2026-03-01T10:15:30Z"),
            )
        }
        assertThat(thrown).hasRootCauseMessage(
            "Can't infer the SQL type to use for an instance of java.time.Instant. Use setObject() with an explicit Types value to specify the type to use.",
        )
    }

    @Test
    fun fixes_pgjdbc_instant_binding_with_offset_date_time() {
        val supported = OffsetDateTime.parse("2026-03-01T10:15:30Z")
        assertThat(
            jdbcTemplate.queryForObject(
                "select cast(? as timestamptz) is null",
                Boolean::class.java,
                supported,
            ),
        ).isFalse
    }

    companion object {
        @JvmStatic
        fun scalarCases(): Stream<Arguments> = Stream.of(
            Arguments.of("String", "text", "value"),
            Arguments.of("UUID", "uuid", UUID.fromString("10000000-0000-0000-0000-000000000003")),
            Arguments.of("Boolean", "boolean", true),
            Arguments.of("Int", "integer", 42),
            Arguments.of("Long", "bigint", 42L),
            Arguments.of("BigDecimal", "numeric", BigDecimal("42.50")),
            Arguments.of("LocalDate", "date", LocalDate.parse("2026-03-02")),
            Arguments.of("OffsetDateTime", "timestamptz", OffsetDateTime.parse("2026-03-02T12:00:00Z")),
            Arguments.of("enum text", "querystatus", "DONE"),
            Arguments.of("JSONB", "jsonb", "{\"present\":true}"),
        )
    }
}
