package com.example.querylab.entity

import jakarta.persistence.Column
import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.util.UUID

@Entity
@Table(name = "converted_sample")
class ConvertedSample(
    @field:Id
    var id: UUID,

    @field:Convert(converter = QueryStatusConverter::class)
    @field:Column(name = "status_code")
    var status: QueryStatus? = null,
)

