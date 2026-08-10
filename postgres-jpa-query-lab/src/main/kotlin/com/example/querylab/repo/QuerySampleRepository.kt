package com.example.querylab.repo

import com.example.querylab.entity.QuerySample
import com.example.querylab.entity.QueryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime

interface QuerySampleRepository : JpaRepository<QuerySample, UUID>, JpaSpecificationExecutor<QuerySample> {
    @Query("select q from QuerySample q where (:value is null or q.externalId = :value) order by q.name")
    fun findByNullableExternalId(@Param("value") value: UUID?): List<QuerySample>

    @Query("select q from QuerySample q where (:value is null or q.status = :value) order by q.name")
    fun findByNullableStatus(@Param("value") value: QueryStatus?): List<QuerySample>

    @Query("select q from QuerySample q where q.status = :value order by q.name")
    fun findByStatus(@Param("value") value: QueryStatus): List<QuerySample>

    @Query("select q from QuerySample q where q.id in :ids order by q.name")
    fun findByIds(@Param("ids") ids: Collection<UUID>): List<QuerySample>

    @Query("select q from QuerySample q where q.status in :statuses order by q.name")
    fun findByStatuses(@Param("statuses") statuses: Collection<QueryStatus>): List<QuerySample>

    @Query(
        value = "select * from query_sample q where (cast(:value as uuid) is null or q.external_id = cast(:value as uuid)) order by q.name",
        nativeQuery = true,
    )
    fun findNativeByNullableExternalId(@Param("value") value: UUID?): List<QuerySample>

    @Query(
        value = "select * from query_sample q where (:value::uuid is null or q.external_id = :value::uuid) order by q.name",
        nativeQuery = true,
    )
    fun findNativeWithDoubleColon(@Param("value") value: UUID?): List<QuerySample>

    @Query("select q from QuerySample q where (:value is null or q.active = :value) order by q.name")
    fun findByNullableActive(@Param("value") value: Boolean?): List<QuerySample>

    @Query("select q from QuerySample q where (:value is null or q.amount = :value) order by q.name")
    fun findByNullableAmount(@Param("value") value: BigDecimal?): List<QuerySample>

    @Query("select q from QuerySample q where (:value is null or q.localDate = :value) order by q.name")
    fun findByNullableLocalDate(@Param("value") value: LocalDate?): List<QuerySample>

    @Query("select q from QuerySample q where q.localDate = :value order by q.name")
    fun findByLocalDate(@Param("value") value: LocalDate): List<QuerySample>

    @Query("select q from QuerySample q where (:value is null or q.createdAt = :value) order by q.name")
    fun findByNullableCreatedAt(@Param("value") value: Instant?): List<QuerySample>

    @Query("select q from QuerySample q where q.createdAt = :value order by q.name")
    fun findByCreatedAt(@Param("value") value: Instant): List<QuerySample>

    @Query("select q from QuerySample q where (:value is null or q.processedAt = :value) order by q.name")
    fun findByNullableProcessedAt(@Param("value") value: OffsetDateTime?): List<QuerySample>

    @Query("select q from QuerySample q where q.processedAt = :value order by q.name")
    fun findByProcessedAt(@Param("value") value: OffsetDateTime): List<QuerySample>
}
