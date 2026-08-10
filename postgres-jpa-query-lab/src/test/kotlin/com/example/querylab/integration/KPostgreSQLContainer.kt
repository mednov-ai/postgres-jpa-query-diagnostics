package com.example.querylab.integration

import org.testcontainers.containers.PostgreSQLContainer

class KPostgreSQLContainer(imageName: String) : PostgreSQLContainer<KPostgreSQLContainer>(imageName)

