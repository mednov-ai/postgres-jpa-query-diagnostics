package com.example.querylab.integration

import com.example.querylab.entity.QuerySample
import com.example.querylab.entity.QueryStatus
import com.example.querylab.diagnostics.DiagnosticAssertions.meaningfulSqlException
import com.example.querylab.diagnostics.DiagnosticAssertions.exceptionChain
import com.example.querylab.diagnostics.DiagnosticLayer
import com.example.querylab.diagnostics.EvidenceRecorder
import com.example.querylab.diagnostics.ScenarioEvidence
import com.example.querylab.repo.QuerySampleRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.hibernate.query.QueryArgumentException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.InvalidDataAccessResourceUsageException
import org.springframework.dao.InvalidDataAccessApiUsageException
import java.util.UUID
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

class JpqlAndNativeQueryIT : AbstractPostgresIntegrationTest() {
    @Autowired
    lateinit var repository: QuerySampleRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun observes_jpql_nullable_uuid_is_typed_from_entity_attribute() {
        assertThat(repository.findByNullableExternalId(null)).hasSize(4)
        val value = UUID.fromString("10000000-0000-0000-0000-000000000003")
        assertThat(repository.findByNullableExternalId(value).map { it.name }).containsExactly("ordinary")
    }

    @Test
    fun reproduces_jpql_nullable_native_enum_42p18() {
        val thrown = catchThrowable { repository.findByNullableStatus(null) }
        assertThat(thrown).isInstanceOf(InvalidDataAccessResourceUsageException::class.java)
        val sql = requireNotNull(meaningfulSqlException(thrown))
        assertThat(sql.sqlState).isEqualTo("42P18")
        EvidenceRecorder.record(
            ScenarioEvidence(
                scenarioId = "jpql-nullable-native-enum-42p18",
                variant = "REPRODUCES",
                mechanism = "Spring Data JPQL",
                sourceQuery = "select q from QuerySample q where (:value is null or q.status = :value)",
                sentSql = "select ... from query_sample where (? is null or status=?) order by name",
                kotlinType = "QueryStatus?",
                valueCategory = "NULL",
                expectedPostgresType = "querystatus",
                bindApi = "Spring Data/Hibernate setParameter",
                bindType = "first placeholder unknown; comparison placeholder querystatus",
                exceptionChain = exceptionChain(thrown),
                meaningfulException = sql.javaClass.name,
                sqlState = sql.sqlState,
                postgresMessage = sql.message,
                layer = DiagnosticLayer.POSTGRESQL_PARSER,
            ),
        )
    }

    @Test
    fun fixes_jpql_nullable_native_enum_with_dynamic_predicate() {
        val nullResult = repository.findAll().sortedBy { it.name }
        val valueResult = repository.findByStatus(QueryStatus.DONE)
        assertThat(nullResult).hasSize(4)
        assertThat(valueResult.map { it.name }).containsExactly("ordinary")
        EvidenceRecorder.record(
            ScenarioEvidence(
                scenarioId = "jpql-nullable-native-enum-42p18",
                variant = "FIXES",
                mechanism = "Spring Data JPQL + dynamic method selection",
                sourceQuery = "null -> findAll(); value -> where q.status = :value",
                kotlinType = "QueryStatus?",
                valueCategory = "NULL_AND_VALID",
                expectedPostgresType = "querystatus",
                bindApi = "omit predicate for null",
                layer = DiagnosticLayer.POSTGRESQL_PARSER,
                verifiedResult = "4 rows for null; ordinary row for DONE",
            ),
        )
    }

    @Test
    fun fixes_hibernate_named_parameter_parser_conflict_with_standard_cast() {
        assertThat(repository.findNativeByNullableExternalId(null)).hasSize(4)
        val value = UUID.fromString("10000000-0000-0000-0000-000000000004")
        assertThat(repository.findNativeByNullableExternalId(value).map { it.name }).containsExactly("json-empty-array")
    }

    @Test
    fun reproduces_hibernate_named_parameter_parser_conflict_with_double_colon() {
        val thrown = catchThrowable { repository.findNativeWithDoubleColon(null) }
        assertThat(thrown).isInstanceOf(InvalidDataAccessApiUsageException::class.java)
        assertThat(thrown).hasMessageContaining("value::uuid")
        EvidenceRecorder.record(
            ScenarioEvidence(
                scenarioId = "spring-data-native-double-colon-parser",
                variant = "REPRODUCES",
                mechanism = "Spring Data native query",
                sourceQuery = ":value::uuid",
                kotlinType = "UUID?",
                valueCategory = "NULL",
                expectedPostgresType = "uuid",
                bindApi = "@Param(value)",
                meaningfulException = thrown.javaClass.name,
                layer = DiagnosticLayer.SPRING_DATA,
            ),
        )
    }

    @Test
    fun observes_jpql_boolean_and_numeric_type_inference() {
        assertThat(repository.findByNullableActive(null)).hasSize(4)
        assertThat(repository.findByNullableActive(true).map { it.name }).containsExactly("ordinary")
        assertThat(repository.findByNullableAmount(null)).hasSize(4)
        assertThat(repository.findByNullableAmount(BigDecimal("42.50")).map { it.name }).containsExactly("ordinary")
    }

    @Test
    fun reproduces_jpql_nullable_instant_42p18() {
        val thrown = catchThrowable {
            repository.findByNullableCreatedAt(Instant.parse("2026-03-01T10:15:30Z"))
        }
        assertThat(thrown).isInstanceOf(InvalidDataAccessResourceUsageException::class.java)
        val sql = requireNotNull(meaningfulSqlException(thrown))
        assertThat(sql.sqlState).isEqualTo("42P18")
    }

    @Test
    fun fixes_jpql_nullable_temporal_values_with_dynamic_predicate() {
        val all = repository.findAll()
        val date = repository.findByLocalDate(LocalDate.parse("2026-03-02"))
        val instant = repository.findByCreatedAt(Instant.parse("2026-03-01T10:15:30Z"))
        val offset = repository.findByProcessedAt(OffsetDateTime.parse("2026-03-02T12:00:00Z"))
        assertThat(all).hasSize(4)
        assertThat(date.map { it.name }).containsExactly("ordinary")
        assertThat(instant.map { it.name }).containsExactly("ordinary")
        assertThat(offset.map { it.name }).containsExactly("ordinary")
    }

    @Test
    fun observes_jpql_empty_collection_is_false() {
        assertThat(repository.findByIds(emptyList())).isEmpty()
        val one = repository.findByIds(
            listOf(UUID.fromString("00000000-0000-0000-0000-000000000003")),
        )
        assertThat(one.map { it.name }).containsExactly("ordinary")
    }

    @Test
    fun reproduces_hibernate_parameter_value_type_validation() {
        val query = entityManager.createQuery(
            "select q from QuerySample q where q.externalId = :value",
            QuerySample::class.java,
        )
        @Suppress("UNCHECKED_CAST")
        val untyped = query as jakarta.persistence.Query
        val thrown = catchThrowable {
            untyped.setParameter("value", "not-a-uuid")
            untyped.resultList
        }
        assertThat(thrown).isInstanceOf(QueryArgumentException::class.java)
        assertThat(thrown).hasMessageContaining("did not match parameter type")
        EvidenceRecorder.record(
            ScenarioEvidence(
                scenarioId = "hibernate-query-argument-type-mismatch",
                variant = "REPRODUCES",
                mechanism = "EntityManager JPQL",
                sourceQuery = "where q.externalId = :value",
                kotlinType = "String",
                valueCategory = "INCOMPATIBLE",
                expectedPostgresType = "uuid",
                bindApi = "Query.setParameter(String, String)",
                exceptionChain = exceptionChain(thrown),
                meaningfulException = thrown.javaClass.name,
                layer = DiagnosticLayer.HIBERNATE_TYPE_SYSTEM,
            ),
        )
    }

    @Test
    fun fixes_hibernate_parameter_with_matching_java_type() {
        val value = UUID.fromString("10000000-0000-0000-0000-000000000003")
        val result = entityManager.createQuery(
            "select q from QuerySample q where q.externalId = :value",
            QuerySample::class.java,
        ).setParameter("value", value)
            .resultList
        assertThat(result.map { it.name }).containsExactly("ordinary")
        EvidenceRecorder.record(
            ScenarioEvidence(
                scenarioId = "hibernate-query-argument-type-mismatch",
                variant = "FIXES",
                mechanism = "EntityManager JPQL",
                sourceQuery = "where q.externalId = :value",
                kotlinType = "UUID",
                valueCategory = "VALID",
                expectedPostgresType = "uuid",
                bindApi = "Query.setParameter(String, UUID)",
                layer = DiagnosticLayer.HIBERNATE_TYPE_SYSTEM,
                verifiedResult = "ordinary row",
            ),
        )
    }

    @Test
    fun observes_coalesce_changes_null_column_semantics() {
        val orNull = entityManager.createNativeQuery(
            "select count(*) from query_sample where (cast(:value as uuid) is null or external_id = cast(:value as uuid))",
        ).setParameter("value", null).singleResult as Number
        val coalesce = entityManager.createNativeQuery(
            "select count(*) from query_sample where external_id = coalesce(cast(:value as uuid), external_id)",
        ).setParameter("value", null).singleResult as Number

        assertThat(orNull.toInt()).isEqualTo(4)
        assertThat(coalesce.toInt()).isEqualTo(3)
    }
}
