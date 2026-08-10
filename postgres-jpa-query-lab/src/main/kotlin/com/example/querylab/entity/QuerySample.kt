package com.example.querylab.entity

import com.fasterxml.jackson.databind.JsonNode
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.util.UUID

@Entity
@Table(name = "query_sample")
class QuerySample(
    @field:Id
    var id: UUID,

    var name: String,

    var description: String? = null,

    @field:Enumerated(EnumType.STRING)
    @field:JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @field:Column(columnDefinition = "querystatus")
    var status: QueryStatus,

    @field:Column(name = "external_id")
    var externalId: UUID? = null,

    @field:Column(name = "created_at")
    var createdAt: Instant,

    @field:Column(name = "processed_at")
    var processedAt: OffsetDateTime? = null,

    @field:Column(name = "local_date")
    var localDate: LocalDate? = null,

    var amount: BigDecimal? = null,

    var active: Boolean? = null,

    @field:JdbcTypeCode(SqlTypes.JSON)
    @field:Column(columnDefinition = "jsonb")
    var metadata: JsonNode? = null,

    @field:Column(columnDefinition = "text[]")
    var tags: Array<String>? = null,

    @field:Column(name = "related_ids", columnDefinition = "uuid[]")
    var relatedIds: Array<UUID>? = null,
)
