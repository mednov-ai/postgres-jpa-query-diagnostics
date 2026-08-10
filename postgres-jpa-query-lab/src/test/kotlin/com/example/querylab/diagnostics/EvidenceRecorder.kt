package com.example.querylab.diagnostics

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption

data class ScenarioEvidence(
    val scenarioId: String,
    val variant: String,
    val mechanism: String,
    val sourceQuery: String,
    val sentSql: String? = null,
    val kotlinType: String,
    val valueCategory: String,
    val expectedPostgresType: String,
    val bindApi: String,
    val bindType: String? = null,
    val exceptionChain: List<String> = emptyList(),
    val meaningfulException: String? = null,
    val sqlState: String? = null,
    val postgresMessage: String? = null,
    val layer: DiagnosticLayer,
    val verifiedResult: String? = null,
    val semanticChange: Boolean = false,
)

object EvidenceRecorder {
    private val mapper = jacksonObjectMapper()
    private val output: Path = Path.of(
        System.getProperty("diagnostic.output.dir", "test-results"),
        "diagnostic-evidence.jsonl",
    )
    private var initialized = false

    @Synchronized
    fun record(evidence: ScenarioEvidence) {
        if (!initialized) {
            Files.createDirectories(output.parent)
            Files.deleteIfExists(output)
            initialized = true
        }
        Files.writeString(
            output,
            mapper.writeValueAsString(evidence) + System.lineSeparator(),
            StandardOpenOption.CREATE,
            StandardOpenOption.APPEND,
        )
    }
}
