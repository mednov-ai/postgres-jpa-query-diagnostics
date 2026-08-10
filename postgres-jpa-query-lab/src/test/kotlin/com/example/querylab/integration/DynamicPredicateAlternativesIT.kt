package com.example.querylab.integration

import com.example.querylab.entity.QuerySample
import com.example.querylab.entity.QueryStatus
import com.example.querylab.repo.QuerySampleRepository
import jakarta.persistence.EntityManager
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.jpa.domain.Specification

class DynamicPredicateAlternativesIT : AbstractPostgresIntegrationTest() {
    @Autowired
    lateinit var repository: QuerySampleRepository

    @Autowired
    lateinit var entityManager: EntityManager

    @Test
    fun specification_omits_predicate_for_null_and_types_enum_for_value() {
        fun specification(status: QueryStatus?): Specification<QuerySample> = Specification { root, _, cb ->
            if (status == null) cb.conjunction() else cb.equal(root.get<QueryStatus>("status"), status)
        }

        assertThat(repository.findAll(specification(null))).hasSize(4)
        assertThat(repository.findAll(specification(QueryStatus.DONE)).map { it.name }).containsExactly("ordinary")
    }

    @Test
    fun criteria_api_omits_predicate_for_null_and_types_enum_for_value() {
        fun query(status: QueryStatus?): List<QuerySample> {
            val cb = entityManager.criteriaBuilder
            val criteria = cb.createQuery(QuerySample::class.java)
            val root = criteria.from(QuerySample::class.java)
            criteria.select(root)
            if (status != null) {
                criteria.where(cb.equal(root.get<QueryStatus>("status"), status))
            }
            criteria.orderBy(cb.asc(root.get<String>("name")))
            return entityManager.createQuery(criteria).resultList
        }

        assertThat(query(null)).hasSize(4)
        assertThat(query(QueryStatus.DONE).map { it.name }).containsExactly("ordinary")
    }
}

