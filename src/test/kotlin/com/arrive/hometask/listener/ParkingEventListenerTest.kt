package com.arrive.hometask.listener

import com.arrive.hometask.config.BaseTestConfig
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.core.io.ClassPathResource
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import org.springframework.util.StreamUtils.copyToString
import java.nio.charset.Charset
import java.util.concurrent.TimeUnit

/**
 * Unit Test for ParkingEventListener with Embedded Kafka and Testcontainers PostgreSQL
 *
 * This test demonstrates how to use:
 * - EmbeddedKafka for in-memory Kafka broker (fast, no Docker)
 * - Testcontainers PostgreSQL for real database (Docker required)
 *
 * This ensures tests run against the same database type as production.
 */
@SpringBootTest
@ActiveProfiles("test")
@EmbeddedKafka(
    partitions = 1,
    topics = ["parking.events"],
    brokerProperties = [
        "listeners=PLAINTEXT://localhost:9093",
        "port=9093"
    ]
)
class ParkingEventListenerTest : BaseTestConfig() {

    @Autowired(required = false)
    private lateinit var kafkaTemplate: KafkaTemplate<String?, String?>

    @Test
    fun `should receive parking started event from embedded Kafka`() {
        // Given: A parking started event
        val startMessage = copyToString(ClassPathResource("example-parking-started-event.json").inputStream, Charset.defaultCharset())


        // When: Event is published to Kafka topic
        kafkaTemplate.send("parking.events", "testKey", startMessage)
            .get(10, TimeUnit.SECONDS)

        // Then: The listener should process the event
        // TODO: Add verification once listener is implemented
        // For now, this test verifies that:
        // 1. Embedded Kafka broker starts successfully
        // 2. PostgreSQL container starts successfully
        // 3. Messages can be sent to the topic
        // 4. No exceptions are thrown
    }
}
