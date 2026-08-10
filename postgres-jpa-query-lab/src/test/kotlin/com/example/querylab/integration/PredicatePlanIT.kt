package com.example.querylab.integration

import com.example.querylab.diagnostics.DiagnosticLayer
import com.example.querylab.diagnostics.EvidenceRecorder
import com.example.querylab.diagnostics.ScenarioEvidence
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PredicatePlanIT : AbstractPostgresIntegrationTest() {
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @BeforeAll
    fun preparePlanData() {
        jdbcTemplate.execute("drop table if exists plan_query_sample")
        jdbcTemplate.execute("create table plan_query_sample (id uuid primary key, external_id uuid)")
        jdbcTemplate.execute("create index idx_plan_external_id on plan_query_sample (external_id)")
        jdbcTemplate.execute(
            """
            insert into plan_query_sample (id, external_id)
            select gen_random_uuid(), gen_random_uuid()
            from generate_series(1, 5000) i
            """.trimIndent(),
        )
        jdbcTemplate.execute("analyze plan_query_sample")
    }

    @Test
    fun parameter_cast_preserves_index_eligibility_but_column_cast_does_not() {
        val target = jdbcTemplate.queryForObject(
            "select external_id::text from plan_query_sample limit 1",
            String::class.java,
        )!!
        val parameterPlan = explain(
            "select * from plan_query_sample where external_id = cast('$target' as uuid)",
        )
        val columnPlan = explain(
            "select * from plan_query_sample where cast(external_id as text) = '$target'",
        )

        assertThat(parameterPlan).contains("idx_plan_external_id")
        assertThat(columnPlan).doesNotContain("idx_plan_external_id")
        assertThat(columnPlan).contains("Seq Scan")
        EvidenceRecorder.record(
            ScenarioEvidence(
                scenarioId = "parameter-cast-versus-column-cast-plan",
                variant = "OBSERVES",
                mechanism = "EXPLAIN (ANALYZE, BUFFERS)",
                sourceQuery = "external_id = CAST(parameter AS uuid) vs CAST(external_id AS text) = parameter",
                kotlinType = "String UUID literal",
                valueCategory = "VALID",
                expectedPostgresType = "uuid",
                bindApi = "SQL literal for deterministic plan",
                layer = DiagnosticLayer.POSTGRESQL_OPERATOR_RESOLUTION,
                verifiedResult = "parameter cast plan: $parameterPlan\ncolumn cast plan: $columnPlan",
            ),
        )
    }

    @Test
    fun explains_or_null_and_coalesce_with_analyze_and_buffers() {
        val target = "10000000-0000-0000-0000-000000000003"
        val orPlan = explain(
            "select * from plan_query_sample where (cast('$target' as uuid) is null or external_id = cast('$target' as uuid))",
        )
        val coalescePlan = explain(
            "select * from plan_query_sample where external_id = coalesce(cast('$target' as uuid), external_id)",
        )
        assertThat(orPlan).contains("idx_plan_external_id")
        assertThat(coalescePlan).isNotBlank()
    }

    private fun explain(sql: String): String = jdbcTemplate.queryForList(
        "explain (analyze, buffers, format text) $sql",
        String::class.java,
    ).joinToString(System.lineSeparator())
}
