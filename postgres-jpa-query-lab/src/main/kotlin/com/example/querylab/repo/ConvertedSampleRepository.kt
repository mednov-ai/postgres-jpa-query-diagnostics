package com.example.querylab.repo

import com.example.querylab.entity.ConvertedSample
import com.example.querylab.entity.QueryStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface ConvertedSampleRepository : JpaRepository<ConvertedSample, UUID> {
    @Query("select c from ConvertedSample c where (:status is null or c.status = :status) order by c.id")
    fun findByNullableStatus(@Param("status") status: QueryStatus?): List<ConvertedSample>
}

