package com.example.querylab.integration

import com.example.querylab.diagnostics.DiagnosticAssertions.expectSqlState
import com.example.querylab.diagnostics.DiagnosticAssertions.recordFix
import com.example.querylab.diagnostics.DiagnosticLayer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import javax.sql.DataSource
import java.sql.Types
import java.util.UUID

class JdbcParameterInferenceIT : AbstractPostgresIntegrationTest() {
    @Autowired
    lateinit var dataSource: DataSource

    @Test
    fun reproduces_unknown_null_parameter_42p18() {
        val sql = "select ? is null"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setNull(1, Types.NULL)
                expectSqlState(
                    "unknown-null-42p18", sql, "42P18", DiagnosticLayer.POSTGRESQL_PARSER,
                    "Nothing?", "NULL", "unknown", "setNull(Types.NULL)",
                ) { statement.executeQuery() }
            }
        }
    }

    @Test
    fun fixes_unknown_null_parameter_with_sql_cast() {
        val sql = "select cast(? as uuid) is null"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setNull(1, Types.NULL)
                statement.executeQuery().use { result ->
                    assertThat(result.next()).isTrue
                    assertThat(result.getBoolean(1)).isTrue
                }
            }
        }
        recordFix(
            "unknown-null-42p18", sql, "UUID?", "NULL", "uuid",
            "setNull(Types.NULL) + CAST parameter", "true",
        )
    }

    @Test
    fun fixes_unknown_null_parameter_with_typed_set_null() {
        val sql = "select ? is null"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setNull(1, Types.OTHER, "uuid")
                statement.executeQuery().use { result ->
                    assertThat(result.next()).isTrue
                    assertThat(result.getBoolean(1)).isTrue
                }
            }
        }
        recordFix(
            "unknown-null-42p18-typed-null", sql, "UUID?", "NULL", "uuid",
            "setNull(Types.OTHER, uuid)", "true",
        )
    }

    @Test
    fun reproduces_operator_resolution_42883() {
        val sql = "select count(*) from query_sample where external_id = ?"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setBytes(1, byteArrayOf(1, 2, 3))
                expectSqlState(
                    "uuid-bytea-operator-42883", sql, "42883",
                    DiagnosticLayer.POSTGRESQL_OPERATOR_RESOLUTION,
                    "ByteArray", "INCOMPATIBLE", "uuid", "setBytes",
                ) { statement.executeQuery() }
            }
        }
    }

    @Test
    fun fixes_operator_resolution_with_uuid_binding() {
        val sql = "select count(*) from query_sample where external_id = ?"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, UUID.fromString("10000000-0000-0000-0000-000000000003"))
                statement.executeQuery().use { result ->
                    assertThat(result.next()).isTrue
                    assertThat(result.getInt(1)).isEqualTo(1)
                }
            }
        }
        recordFix(
            "uuid-bytea-operator-42883", sql, "UUID", "VALID", "uuid",
            "setObject(UUID)", "one matching row",
        )
    }

    @Test
    fun reproduces_parameter_in_identifier_position_42601() {
        val sql = "select * from ?"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, "query_sample")
                expectSqlState(
                    "parameter-as-identifier-42601", sql, "42601", DiagnosticLayer.POSTGRESQL_PARSER,
                    "String", "VALID", "identifier", "setString",
                ) { statement.executeQuery() }
            }
        }
    }

    @Test
    fun reproduces_no_value_specified_for_parameter() {
        val sql = "select ?"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                val thrown = org.assertj.core.api.Assertions.catchThrowable { statement.executeQuery() }
                val exception = requireNotNull(
                    com.example.querylab.diagnostics.DiagnosticAssertions.meaningfulSqlException(thrown),
                )
                assertThat(exception.message).contains("No value specified for parameter 1")
            }
        }
    }

    @Test
    fun fixes_no_value_specified_with_explicit_binding() {
        val sql = "select ?"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setInt(1, 42)
                statement.executeQuery().use { result ->
                    assertThat(result.next()).isTrue
                    assertThat(result.getInt(1)).isEqualTo(42)
                }
            }
        }
    }
}
