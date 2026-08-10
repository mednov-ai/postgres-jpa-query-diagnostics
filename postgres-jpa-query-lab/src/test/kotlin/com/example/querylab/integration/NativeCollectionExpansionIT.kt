package com.example.querylab.integration

import com.example.querylab.diagnostics.DiagnosticAssertions.meaningfulSqlException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.util.UUID

class NativeCollectionExpansionIT : AbstractPostgresIntegrationTest() {
    @Autowired
    lateinit var namedJdbcTemplate: NamedParameterJdbcTemplate

    @Test
    fun observes_named_parameter_in_with_one_and_many_values() {
        val sql = "select count(*) from query_sample where external_id in (:values)"
        val one = listOf(UUID.fromString("10000000-0000-0000-0000-000000000003"))
        val many = listOf(
            UUID.fromString("10000000-0000-0000-0000-000000000002"),
            UUID.fromString("10000000-0000-0000-0000-000000000003"),
        )
        assertThat(namedJdbcTemplate.queryForObject(sql, MapSqlParameterSource("values", one), Int::class.java)).isEqualTo(1)
        assertThat(namedJdbcTemplate.queryForObject(sql, MapSqlParameterSource("values", many), Int::class.java)).isEqualTo(2)
    }

    @Test
    fun reproduces_named_parameter_in_with_empty_list_42601() {
        val sql = "select count(*) from query_sample where external_id in (:values)"
        val thrown = catchThrowable {
            namedJdbcTemplate.queryForObject(
                sql,
                MapSqlParameterSource("values", emptyList<UUID>()),
                Int::class.java,
            )
        }
        assertThat(requireNotNull(meaningfulSqlException(thrown)).sqlState).isEqualTo("42601")
    }

    @Test
    fun fixes_named_parameter_empty_list_by_short_circuiting() {
        fun count(ids: List<UUID>): Int = if (ids.isEmpty()) {
            0
        } else {
            namedJdbcTemplate.queryForObject(
                "select count(*) from query_sample where external_id in (:values)",
                MapSqlParameterSource("values", ids),
                Int::class.java,
            )!!
        }
        assertThat(count(emptyList())).isZero()
        assertThat(count(listOf(UUID.fromString("10000000-0000-0000-0000-000000000003")))).isEqualTo(1)
    }
}

