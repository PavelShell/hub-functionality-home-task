package com.arrive.hometask.listener

import com.arrive.hometask.config.IntegrationTestConfig
import com.arrive.hometask.config.KafkaProducerTestConfig
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

    @Test
    fun `should process parking started event end-to-end`() {
        // Given: A parking started event
        val startMessage = copyToString(ClassPathResource("example-parking-started-event.json").inputStream, Charset.defaultCharset())

        // When: Event is published to Kafka topic
        testKafkaTemplate.send("parking.events", "testKey", startMessage)
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
//
//    @Test
//    fun `should handle error gracefully`() {
//        val result = testKafkaTemplate.send("parking.events", "testKey", "foobar")
//            .get(10, TimeUnit.SECONDS)
//        println(result)
//    }
}
