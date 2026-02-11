package com.arrive.hometask.listener

import com.arrive.hometask.config.IntegrationTestConfig
import com.arrive.hometask.config.KafkaProducerTestConfig
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.core.io.ClassPathResource
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.StreamUtils.copyToString
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * Integration Test for ParkingEventListener with Testcontainers
 *
 * This test demonstrates how to use Testcontainers for integration testing.
 * Uses real Kafka and PostgreSQL containers for end-to-end testing.
 */
@SpringBootTest
@ActiveProfiles("integration-test")
@Import(KafkaProducerTestConfig::class)
class ParkingEventListenerIntegrationTest : IntegrationTestConfig() {

    @Autowired
    private lateinit var testKafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Test
    fun `should process parking started event end-to-end`() {
        // Given: A parking started event
        val event = objectMapper.readValue(
            copyToString(ClassPathResource("example-parking-started-event.json").inputStream, Charset.defaultCharset()),
            ParkingEvent::class.java
        )

        // When: Event is published to Kafka topic
        testKafkaTemplate.send("parking.events", "testKey", event)
            .get(10, TimeUnit.SECONDS)

        // Then: The listener should process the event and:
        // TODO: Add verification once listener is implemented
        // 1. Save to database
        // 2. Call SimplePark API
        // 3. Update status accordingly

        // For now, this test verifies that:
        // 1. Testcontainers start successfully
        // 2. Kafka container is accessible
        // 3. PostgreSQL container is accessible
        // 4. Messages can be sent to Kafka
    }
}
