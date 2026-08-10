package com.example.querylab.integration

import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource

@SpringBootTest
abstract class AbstractPostgresIntegrationTest {
    companion object {
        val postgres = KPostgreSQLContainer("postgres:18.4")
            .withDatabaseName("query_lab")
            .withUsername("query_lab")
            .withPassword("query_lab")
            .apply { start() }

        @DynamicPropertySource
        @JvmStatic
        fun postgresProperties(registry: DynamicPropertyRegistry) {
            registry.add("spring.datasource.url", postgres::getJdbcUrl)
            registry.add("spring.datasource.username", postgres::getUsername)
            registry.add("spring.datasource.password", postgres::getPassword)
        }
    }
}
