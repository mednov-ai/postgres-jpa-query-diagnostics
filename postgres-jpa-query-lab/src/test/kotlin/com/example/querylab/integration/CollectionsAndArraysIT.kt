package com.example.querylab.integration

import com.example.querylab.diagnostics.DiagnosticAssertions.expectSqlState
import com.example.querylab.diagnostics.DiagnosticAssertions.recordFix
import com.example.querylab.diagnostics.DiagnosticLayer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.springframework.beans.factory.annotation.Autowired
import java.sql.Types
import java.util.UUID
import java.util.stream.Stream
import javax.sql.DataSource

class CollectionsAndArraysIT : AbstractPostgresIntegrationTest() {
    @Autowired
    lateinit var dataSource: DataSource

    @ParameterizedTest(name = "ANY uuid array: {0}")
    @MethodSource("uuidArrayCases")
    fun observes_null_empty_and_non_empty_uuid_arrays(
        label: String,
        values: Array<UUID>?,
        expectedCount: Int,
    ) {
        val sql = "select count(*) from query_sample where external_id = any(?)"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                if (values == null) {
                    statement.setNull(1, Types.ARRAY, "uuid[]")
                } else {
                    statement.setArray(1, connection.createArrayOf("uuid", values))
                }
                statement.executeQuery().use { result ->
                    assertThat(result.next()).isTrue
                    assertThat(result.getInt(1)).`as`(label).isEqualTo(expectedCount)
                }
            }
        }
    }

    @Test
    fun reproduces_any_with_non_array_42809() {
        val sql = "select count(*) from query_sample where external_id = any(?)"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setBytes(1, byteArrayOf(1))
                expectSqlState(
                    "any-non-array-42809", sql, "42809", DiagnosticLayer.POSTGRESQL_OPERATOR_RESOLUTION,
                    "ByteArray", "INCOMPATIBLE", "uuid[]", "setBytes",
                ) { statement.executeQuery() }
            }
        }
    }

    @Test
    fun fixes_any_with_create_array_of() {
        val sql = "select count(*) from query_sample where external_id = any(?)"
        val value = UUID.fromString("10000000-0000-0000-0000-000000000003")
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setArray(1, connection.createArrayOf("uuid", arrayOf(value)))
                statement.executeQuery().use { result ->
                    assertThat(result.next()).isTrue
                    assertThat(result.getInt(1)).isEqualTo(1)
                }
            }
        }
        recordFix(
            "any-non-array-42809", sql, "Array<UUID>", "VALID", "uuid[]",
            "setArray(createArrayOf(uuid, values))", "one matching row",
        )
    }

    @Test
    fun reproduces_uuid_array_assignment_from_bytea_42804() {
        val sql = "update query_sample set related_ids = ? where id = '00000000-0000-0000-0000-000000000001'"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setBytes(1, byteArrayOf(1))
                expectSqlState(
                    "uuid-array-assignment-bytea-42804", sql, "42804", DiagnosticLayer.POSTGRESQL_ASSIGNMENT,
                    "ByteArray", "INCOMPATIBLE", "uuid[]", "setBytes",
                ) { statement.executeUpdate() }
            }
        }
    }

    @Test
    fun fixes_uuid_array_assignment_with_typed_null_and_create_array_of() {
        val sql = "update query_sample set related_ids = ? where id = '00000000-0000-0000-0000-000000000001' returning related_ids"
        val value = UUID.fromString("20000000-0000-0000-0000-000000000099")
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(sql).use { statement ->
                    statement.setNull(1, Types.ARRAY, "uuid[]")
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue
                        assertThat(result.getArray(1)).isNull()
                    }
                    statement.setArray(1, connection.createArrayOf("uuid", arrayOf(value)))
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue
                        val returned = result.getArray(1).array as Array<*>
                        assertThat(returned.map { it.toString() }).containsExactly(value.toString())
                    }
                }
            } finally {
                connection.rollback()
            }
        }
        recordFix(
            "uuid-array-assignment-bytea-42804", sql, "Array<UUID>?", "NULL_AND_VALID", "uuid[]",
            "setNull(Types.ARRAY, uuid[]) / setArray(createArrayOf)", "RETURNING null and exact array",
        )
    }

    @Test
    fun observes_empty_any_and_all_are_not_null_semantics() {
        dataSource.connection.use { connection ->
            val empty = connection.createArrayOf("uuid", emptyArray<UUID>())
            connection.prepareStatement(
                "select count(*) filter (where external_id = any(?)), count(*) filter (where external_id <> all(?)) from query_sample",
            ).use { statement ->
                statement.setArray(1, empty)
                statement.setArray(2, empty)
                statement.executeQuery().use { result ->
                    assertThat(result.next()).isTrue
                    assertThat(result.getInt(1)).isZero()
                    // x <> ALL(empty) is true even for a NULL x: no comparison yields UNKNOWN.
                    assertThat(result.getInt(2)).isEqualTo(4)
                }
            }
        }
    }

    @Test
    fun reproduces_array_position_with_wrong_element_type_42883() {
        val sql = "select array_position(related_ids, ?) from query_sample"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setBytes(1, byteArrayOf(1))
                expectSqlState(
                    "array-position-wrong-element-42883", sql, "42883",
                    DiagnosticLayer.POSTGRESQL_OPERATOR_RESOLUTION,
                    "ByteArray", "INCOMPATIBLE", "uuid", "setBytes",
                ) { statement.executeQuery() }
            }
        }
    }

    @Test
    fun fixes_array_position_with_uuid_element() {
        val sql = "select count(*) from query_sample where array_position(related_ids, ?) is not null"
        val value = UUID.fromString("20000000-0000-0000-0000-000000000001")
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setObject(1, value)
                statement.executeQuery().use { result ->
                    assertThat(result.next()).isTrue
                    assertThat(result.getInt(1)).isEqualTo(1)
                }
            }
        }
        recordFix(
            "array-position-wrong-element-42883", sql, "UUID", "VALID", "uuid",
            "setObject(UUID)", "one array contains UUID",
        )
    }

    @Test
    fun observes_overlap_contains_and_contained_by() {
        dataSource.connection.use { connection ->
            val array = connection.createArrayOf("text", arrayOf("alpha"))
            val expectations = mapOf(
                "tags && ?" to 1,
                "tags @> ?" to 1,
                // The seeded empty PostgreSQL array is a subset of every array.
                "tags <@ ?" to 1,
            )
            expectations.forEach { (expression, expected) ->
                connection.prepareStatement("select count(*) from query_sample where $expression").use { statement ->
                    statement.setArray(1, array)
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue
                        assertThat(result.getInt(1)).isEqualTo(expected)
                    }
                }
            }
        }
    }

    companion object {
        @JvmStatic
        fun uuidArrayCases(): Stream<Arguments> = Stream.of(
            Arguments.of("null", null, 0),
            Arguments.of("empty", emptyArray<UUID>(), 0),
            Arguments.of(
                "one",
                arrayOf(UUID.fromString("10000000-0000-0000-0000-000000000003")),
                1,
            ),
            Arguments.of(
                "many",
                arrayOf(
                    UUID.fromString("10000000-0000-0000-0000-000000000002"),
                    UUID.fromString("10000000-0000-0000-0000-000000000003"),
                ),
                2,
            ),
        )
    }
}
