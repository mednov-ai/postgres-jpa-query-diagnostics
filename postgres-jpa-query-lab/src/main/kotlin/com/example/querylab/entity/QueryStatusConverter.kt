package com.example.querylab.entity

import jakarta.persistence.AttributeConverter
import jakarta.persistence.Converter

@Converter
class QueryStatusConverter : AttributeConverter<QueryStatus, String> {
    override fun convertToDatabaseColumn(attribute: QueryStatus?): String? = when (attribute) {
        QueryStatus.NEW -> "N"
        QueryStatus.PROCESSING -> "P"
        QueryStatus.DONE -> "D"
        null -> null
    }

    override fun convertToEntityAttribute(dbData: String?): QueryStatus? = when (dbData) {
        "N" -> QueryStatus.NEW
        "P" -> QueryStatus.PROCESSING
        "D" -> QueryStatus.DONE
        null -> null
        else -> error("Unknown query status code: $dbData")
    }
}

