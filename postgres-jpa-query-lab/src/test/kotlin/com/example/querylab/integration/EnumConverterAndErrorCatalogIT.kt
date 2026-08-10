package com.example.querylab.integration

import com.example.querylab.entity.QuerySample
import com.example.querylab.entity.QueryStatus
import com.example.querylab.repo.ConvertedSampleRepository
import com.example.querylab.repo.QuerySampleRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.hibernate.query.SemanticException
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired

class EnumConverterAndErrorCatalogIT : AbstractPostgresIntegrationTest() {
    @Autowired
    lateinit var convertedRepository: ConvertedSampleRepository

    @Autowired
    lateinit var sampleRepository: QuerySampleRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun observes_attribute_converter_nullable_enum() {
        assertThat(convertedRepository.findByNullableStatus(null)).hasSize(3)
        assertThat(convertedRepository.findByNullableStatus(QueryStatus.DONE).map { it.status })
            .containsExactly(QueryStatus.DONE)
    }

    @Test
    fun observes_jpql_native_enum_collection_empty_one_and_many() {
        assertThat(sampleRepository.findByStatuses(emptyList())).isEmpty()
        assertThat(sampleRepository.findByStatuses(listOf(QueryStatus.DONE)).map { it.name })
            .containsExactly("ordinary")
        assertThat(sampleRepository.findByStatuses(listOf(QueryStatus.NEW, QueryStatus.DONE)).map { it.name })
            .containsExactly("json-empty-array", "ordinary", "sql-null")
    }

    @Test
    fun reproduces_named_parameter_not_bound() {
        val query = entityManager.createQuery(
            "select q from QuerySample q where q.externalId = :value",
            QuerySample::class.java,
        )
        val thrown = catchThrowable { query.resultList }
        assertThat(thrown).hasMessageContaining("No argument for named parameter ':value'")
    }

    @Test
    fun reproduces_hql_semantic_exception_before_sql() {
        val thrown = catchThrowable {
            entityManager.createQuery(
                "select q from QuerySample q where q.externalId = q.name",
                QuerySample::class.java,
            )
        }
        assertThat(thrown).hasRootCauseInstanceOf(SemanticException::class.java)
    }
}

