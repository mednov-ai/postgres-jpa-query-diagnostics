package com.example.querylab.integration

import org.assertj.core.api.Assertions.assertThat
import org.flywaydb.core.Flyway
import org.hibernate.Version
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.SpringBootVersion
import org.springframework.jdbc.core.JdbcTemplate

class FixtureAndVersionsIT : AbstractPostgresIntegrationTest() {
    @Autowired
    lateinit var jdbcTemplate: JdbcTemplate

    @Test
    fun verifies_exact_managed_component_versions_and_postgresql() {
        assertThat(SpringBootVersion.getVersion()).isEqualTo("3.5.16")
        assertThat(Version.getVersionString()).isEqualTo("6.6.53.Final")
        assertThat(jdbcTemplate.dataSource!!.connection.use { it.metaData.driverVersion }).isEqualTo("42.7.11")
        assertThat(Flyway::class.java.protectionDomain.codeSource.location.toString()).contains("flyway-core-11.7.2")
        assertThat(jdbcTemplate.queryForObject("show server_version", String::class.java)).startsWith("18.4")
    }

    @Test
    fun fixture_distinguishes_null_empty_and_ordinary_values() {
        assertThat(jdbcTemplate.queryForObject("select count(*) from query_sample where description is null", Int::class.java)).isEqualTo(2)
        assertThat(jdbcTemplate.queryForObject("select count(*) from query_sample where description = ''", Int::class.java)).isEqualTo(1)
        assertThat(jdbcTemplate.queryForObject("select count(*) from query_sample where metadata = '{}'::jsonb", Int::class.java)).isEqualTo(1)
        assertThat(jdbcTemplate.queryForObject("select count(*) from query_sample where metadata ? 'present'", Int::class.java)).isEqualTo(1)
        assertThat(jdbcTemplate.queryForObject("select count(*) from query_sample where tags = '{}'::text[]", Int::class.java)).isEqualTo(1)
        assertThat(jdbcTemplate.queryForObject("select count(*) from query_sample where cardinality(tags) > 0", Int::class.java)).isEqualTo(2)
    }
}
