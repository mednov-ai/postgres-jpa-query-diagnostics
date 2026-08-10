package com.example.querylab.integration

import com.example.querylab.diagnostics.DiagnosticAssertions.expectSqlState
import com.example.querylab.diagnostics.DiagnosticAssertions.recordFix
import com.example.querylab.diagnostics.DiagnosticLayer
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import org.springframework.beans.factory.annotation.Autowired
import java.sql.Types
import java.util.UUID
import javax.sql.DataSource

class NativeDmlBindingIT : AbstractPostgresIntegrationTest() {
    @Autowired
    lateinit var dataSource: DataSource

    @Test
    fun reproduces_uuid_assignment_from_bytea_42804() {
        val sql = "update query_sample set external_id = ? where id = '00000000-0000-0000-0000-000000000001'"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setBytes(1, byteArrayOf(1, 2, 3))
                expectSqlState(
                    "uuid-assignment-bytea-42804", sql, "42804", DiagnosticLayer.POSTGRESQL_ASSIGNMENT,
                    "ByteArray", "INCOMPATIBLE", "uuid", "setBytes",
                ) { statement.executeUpdate() }
            }
        }
    }

    @Test
    fun fixes_uuid_assignment_with_typed_null_and_value() {
        val id = "00000000-0000-0000-0000-000000000001"
        val sql = "update query_sample set external_id = ? where id = '$id' returning external_id"
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(sql).use { statement ->
                    statement.setNull(1, Types.OTHER, "uuid")
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue
                        assertThat(result.getObject(1)).isNull()
                    }
                    val value = UUID.fromString("10000000-0000-0000-0000-000000000099")
                    statement.setObject(1, value)
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue
                        assertThat(result.getObject(1, UUID::class.java)).isEqualTo(value)
                    }
                }
            } finally {
                connection.rollback()
            }
        }
        recordFix(
            "uuid-assignment-bytea-42804", sql, "UUID?", "NULL_AND_VALID", "uuid",
            "setNull(Types.OTHER, uuid) / setObject(UUID)", "RETURNING null and exact UUID",
        )
    }

    @Test
    fun reproduces_jsonb_assignment_from_varchar_42804() {
        val sql = "update query_sample set metadata = ? where id = '00000000-0000-0000-0000-000000000001'"
        dataSource.connection.use { connection ->
            connection.prepareStatement(sql).use { statement ->
                statement.setString(1, "{\"verified\":true}")
                expectSqlState(
                    "jsonb-assignment-varchar-42804", sql, "42804", DiagnosticLayer.POSTGRESQL_ASSIGNMENT,
                    "String", "VALID", "jsonb", "setString",
                ) { statement.executeUpdate() }
            }
        }
    }

    @Test
    fun fixes_jsonb_assignment_with_pgobject() {
        val sql = "update query_sample set metadata = ? where id = '00000000-0000-0000-0000-000000000001' returning metadata"
        val jsonb = PGobject().apply {
            type = "jsonb"
            value = "{\"verified\":true}"
        }
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(sql).use { statement ->
                    statement.setNull(1, Types.OTHER, "jsonb")
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue
                        assertThat(result.getObject(1)).isNull()
                    }
                    statement.setObject(1, jsonb)
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue
                        assertThat(result.getString(1)).contains("verified")
                    }
                }
            } finally {
                connection.rollback()
            }
        }
        recordFix(
            "jsonb-assignment-varchar-42804", sql, "PGobject?", "NULL_AND_VALID", "jsonb",
            "setNull(Types.OTHER, jsonb) / setObject(PGobject)", "RETURNING null and JSON object",
        )
    }

    @Test
    fun observes_text_assignment_from_bytea_is_accepted_on_postgresql_18() {
        val sql = "update query_sample set description = ? where id = '00000000-0000-0000-0000-000000000001' returning description"
        dataSource.connection.use { connection ->
            connection.autoCommit = false
            try {
                connection.prepareStatement(sql).use { statement ->
                    statement.setBytes(1, byteArrayOf(0x41, 0x42))
                    statement.executeQuery().use { result ->
                        assertThat(result.next()).isTrue
                        assertThat(result.getString(1)).contains("4142")
                    }
                }
            } finally {
                connection.rollback()
            }
        }
        com.example.querylab.diagnostics.EvidenceRecorder.record(
            com.example.querylab.diagnostics.ScenarioEvidence(
                scenarioId = "text-assignment-bytea-unreproduced",
                variant = "OBSERVES",
                mechanism = "PreparedStatement",
                sourceQuery = sql,
                kotlinType = "ByteArray",
                valueCategory = "INCOMPATIBLE",
                expectedPostgresType = "text",
                bindApi = "setBytes",
                layer = DiagnosticLayer.POSTGRESQL_ASSIGNMENT,
                verifiedResult = "accepted and rendered as bytea text",
                semanticChange = true,
            ),
        )
    }
}

