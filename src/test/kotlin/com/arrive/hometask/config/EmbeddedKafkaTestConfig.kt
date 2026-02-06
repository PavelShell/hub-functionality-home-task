package com.arrive.hometask.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Base Test Configuration for Unit Tests
 *
 * Provides PostgreSQL Testcontainer for database testing.
 * Use @EmbeddedKafka annotation on test classes for Kafka broker.
 *
 * This configuration uses Testcontainers for PostgreSQL instead of H2,
 * ensuring tests run against the same database as production.
 */
@Testcontainers
abstract class BaseTestConfig {

    companion object {
        @Container
        @JvmStatic
        val postgresContainer = PostgreSQLContainer(DockerImageName.parse("postgres:15-alpine"))
            .apply {
                withDatabaseName("testdb")
                withUsername("test")
                withPassword("test")
                withReuse(true)
            }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // PostgreSQL properties
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }
            registry.add("spring.datasource.driver-class-name") { "org.postgresql.Driver" }
        }
    }
}

/**
 * Minimal configuration class for test context
 */
@TestConfiguration
class EmbeddedKafkaTestConfig {
    // Configuration is minimal as @EmbeddedKafka on test classes provides the broker
    // PostgreSQL is provided by BaseTestConfig
}
