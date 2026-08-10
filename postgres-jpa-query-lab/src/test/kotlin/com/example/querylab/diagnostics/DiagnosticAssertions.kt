package com.example.querylab.diagnostics

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.junit.jupiter.api.Assertions.assertNotNull
import java.sql.SQLException

object DiagnosticAssertions {
    fun expectSqlState(
        scenarioId: String,
        query: String,
        expectedSqlState: String,
        layer: DiagnosticLayer,
        kotlinType: String,
        valueCategory: String,
        expectedPostgresType: String,
        bindApi: String,
        block: () -> Unit,
    ): SQLException {
        val thrown = catchThrowable(block)
        assertNotNull(thrown, "scenario $scenarioId must fail")
        val sql = requireNotNull(meaningfulSqlException(thrown)) {
            "meaningful SQLException for $scenarioId"
        }
        assertThat(sql.sqlState).isEqualTo(expectedSqlState)
        EvidenceRecorder.record(
            ScenarioEvidence(
                scenarioId = scenarioId,
                variant = "REPRODUCES",
                mechanism = "PreparedStatement",
                sourceQuery = query,
                sentSql = query,
                kotlinType = kotlinType,
                valueCategory = valueCategory,
                expectedPostgresType = expectedPostgresType,
                bindApi = bindApi,
                bindType = bindApi,
                exceptionChain = exceptionChain(thrown),
                meaningfulException = sql.javaClass.name,
                sqlState = sql.sqlState,
                postgresMessage = sql.message,
                layer = layer,
            ),
        )
        return sql
    }

    fun recordFix(
        scenarioId: String,
        query: String,
        kotlinType: String,
        valueCategory: String,
        expectedPostgresType: String,
        bindApi: String,
        result: String,
        semanticChange: Boolean = false,
    ) {
        EvidenceRecorder.record(
            ScenarioEvidence(
                scenarioId = scenarioId,
                variant = "FIXES",
                mechanism = "PreparedStatement",
                sourceQuery = query,
                kotlinType = kotlinType,
                valueCategory = valueCategory,
                expectedPostgresType = expectedPostgresType,
                bindApi = bindApi,
                layer = DiagnosticLayer.JDBC_BINDING,
                verifiedResult = result,
                semanticChange = semanticChange,
            ),
        )
    }

    fun meaningfulSqlException(throwable: Throwable): SQLException? {
        val sqlExceptions = mutableListOf<SQLException>()
        val seen = mutableSetOf<Throwable>()
        var current: Throwable? = throwable
        while (current != null && seen.add(current)) {
            if (current is SQLException) {
                var sql: SQLException? = current
                while (sql != null) {
                    sqlExceptions += sql
                    sql = sql.nextException
                }
            }
            current = current.cause
        }
        return sqlExceptions.lastOrNull { it.sqlState != null } ?: sqlExceptions.lastOrNull()
    }

    fun exceptionChain(throwable: Throwable): List<String> {
        val chain = mutableListOf<String>()
        val seen = mutableSetOf<Throwable>()
        var current: Throwable? = throwable
        while (current != null && seen.add(current)) {
            chain += "${current.javaClass.name}: ${current.message}"
            current = current.cause
        }
        return chain
    }
}
