package com.arrive.hometask.config

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.kafka.core.DefaultKafkaProducerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.testcontainers.containers.KafkaContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.utility.DockerImageName

/**
 * Testcontainers Configuration for Integration Tests
 *
 * Provides real Kafka and PostgreSQL containers for comprehensive integration testing.
 * Extend this class in your integration tests to get automatic container lifecycle management.
 */
@Testcontainers
abstract class IntegrationTestConfig {

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

        @Container
        @JvmStatic
        val kafkaContainer = KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.0"))
            .apply {
                withReuse(true)
            }

        @JvmStatic
        @DynamicPropertySource
        fun configureProperties(registry: DynamicPropertyRegistry) {
            // PostgreSQL properties
            registry.add("spring.datasource.url") { postgresContainer.jdbcUrl }
            registry.add("spring.datasource.username") { postgresContainer.username }
            registry.add("spring.datasource.password") { postgresContainer.password }

            // Kafka properties
            registry.add("spring.kafka.bootstrap-servers") { kafkaContainer.bootstrapServers }
        }
    }
}

/**
 * Test Configuration for Kafka Producer
 * Used in integration tests to send test messages
 */
@TestConfiguration
class KafkaProducerTestConfig {

    @Bean
    fun testKafkaProducerFactory(
        registry: DynamicPropertyRegistry? = null
    ): ProducerFactory<String, Any> {
        val configProps = mutableMapOf<String, Any>(
            org.apache.kafka.clients.producer.ProducerConfig.BOOTSTRAP_SERVERS_CONFIG
                to (IntegrationTestConfig.kafkaContainer.bootstrapServers),
            org.apache.kafka.clients.producer.ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG
                to org.apache.kafka.common.serialization.StringSerializer::class.java,
            org.apache.kafka.clients.producer.ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG
                to org.apache.kafka.common.serialization.StringSerializer::class.java
        )
        return DefaultKafkaProducerFactory(configProps)
    }

    @Bean
    fun testKafkaTemplate(
        testKafkaProducerFactory: ProducerFactory<String, Any>
    ): KafkaTemplate<String, Any> {
        return KafkaTemplate(testKafkaProducerFactory)
    }
}
