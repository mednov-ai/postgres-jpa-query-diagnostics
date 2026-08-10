import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    kotlin("jvm") version "1.9.25"
    kotlin("plugin.spring") version "1.9.25"
    kotlin("plugin.jpa") version "1.9.25"
    id("org.springframework.boot") version "3.5.16"
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.example.querylab"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.flywaydb:flyway-core")
    implementation("org.flywaydb:flyway-database-postgresql")
    runtimeOnly("org.postgresql:postgresql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.postgresql:postgresql")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjsr305=strict")
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
    maxParallelForks = 1
    systemProperty("diagnostic.output.dir", layout.projectDirectory.dir("test-results").asFile.absolutePath)
    testLogging {
        exceptionFormat = TestExceptionFormat.FULL
        events("failed", "skipped")
    }
}

tasks.register("verifyDiagnostics") {
    group = "verification"
    description = "Runs the PostgreSQL 18.4 diagnostic integration suite."
    dependsOn(tasks.test, "writeResolvedVersions")
}

tasks.register("writeResolvedVersions") {
    group = "verification"
    description = "Writes the exact managed runtime and test component versions."
    doLast {
        val selected = (configurations.runtimeClasspath.get().incoming.resolutionResult.allComponents +
            configurations.testRuntimeClasspath.get().incoming.resolutionResult.allComponents)
            .mapNotNull { it.moduleVersion }
            .distinctBy { "${it.group}:${it.name}:${it.version}" }
            .filter {
                (it.group == "org.springframework.boot" && it.name == "spring-boot") ||
                    (it.group == "org.springframework.data" && it.name == "spring-data-jpa") ||
                    (it.group == "org.hibernate.orm" && it.name == "hibernate-core") ||
                    (it.group == "org.postgresql" && it.name == "postgresql") ||
                    (it.group == "org.flywaydb" && it.name == "flyway-core") ||
                    (it.group == "org.testcontainers" && it.name == "testcontainers") ||
                    (it.group == "org.junit.jupiter" && it.name == "junit-jupiter")
            }
            .sortedWith(compareBy({ it.group }, { it.name }))
            .joinToString(System.lineSeparator()) { "${it.group}:${it.name}:${it.version}" }
        val output = layout.projectDirectory.file("test-results/resolved-versions.txt").asFile
        output.parentFile.mkdirs()
        output.writeText(selected + System.lineSeparator())
    }
}
