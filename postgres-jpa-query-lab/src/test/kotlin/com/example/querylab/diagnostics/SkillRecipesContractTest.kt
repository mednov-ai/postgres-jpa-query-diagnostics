package com.example.querylab.diagnostics

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.readText

class SkillRecipesContractTest {
    private val projectDir = Path.of(System.getProperty("user.dir"))
    private val recipes = projectDir.resolve("../postgres-jpa-query-diagnostics/references/verified-recipes.md")
        .normalize()

    @Test
    fun every_verified_recipe_has_versions_and_reproduction_fix_tests() {
        assertThat(recipes).exists()
        val sections = recipes.readText()
            .split(Regex("(?m)^## "))
            .drop(1)

        assertThat(sections).isNotEmpty
        sections.forEach { section ->
            val title = section.lineSequence().first()
            assertThat(section).`as`(title).contains("### Symptom")
            assertThat(section).`as`(title).contains("### Root cause")
            assertThat(section).`as`(title).contains("### Verified fix")
            assertThat(section).`as`(title).contains("### Do not use")
            assertThat(section).`as`(title).contains("- Spring Boot: 3.5.16")
            assertThat(section).`as`(title).contains("- Hibernate: 6.6.53.Final")
            assertThat(section).`as`(title).contains("- pgJDBC: 42.7.11")
            assertThat(section).`as`(title).contains("- PostgreSQL: 18.4")

            val references = Regex("`([A-Za-z0-9]+IT)\\.([a-z0-9_]+)`")
                .findAll(section)
                .map { it.groupValues[1] to it.groupValues[2] }
                .toList()
            assertThat(references.map { it.second }).`as`(title).anyMatch { it.startsWith("reproduces_") }
            assertThat(references.map { it.second }).`as`(title).anyMatch { it.startsWith("fixes_") }
            references.forEach { (className, methodName) ->
                val source = findTestSource(className)
                assertThat(source.readText()).`as`("$title -> $className.$methodName")
                    .contains("fun $methodName(")
            }
        }
    }

    private fun findTestSource(className: String): Path {
        val root = projectDir.resolve("src/test/kotlin")
        return Files.walk(root).use { paths ->
            paths.filter { it.fileName.toString() == "$className.kt" }
                .findFirst()
                .orElseThrow { AssertionError("Missing test source $className.kt") }
        }
    }
}

