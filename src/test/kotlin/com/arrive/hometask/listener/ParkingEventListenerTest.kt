package com.arrive.hometask.listener

import com.arrive.hometask.client.SimpleParkClient
import com.arrive.hometask.client.SimpleParkClientResponse
import com.arrive.hometask.client.SimpleParkParkingStatus
import com.arrive.hometask.config.BaseTestConfig
import com.arrive.hometask.db.SimpleParkParkingJpaRepository
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
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

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired(required = false)
    private lateinit var kafkaTemplate: KafkaTemplate<String?, String?>

    @Autowired(required = false)
    private lateinit var parkParkingJpaRepository: SimpleParkParkingJpaRepository

    @MockBean
    private lateinit var parkClient: SimpleParkClient

    @BeforeEach
    @AfterEach
    fun `clean up database`() {
        parkParkingJpaRepository.deleteAllInBatch()
    }

    @Test
    fun `should successfully send message to the topic`() {
        val startMessage =
            copyToString(ClassPathResource("example-parking-started-event.json").inputStream, Charset.defaultCharset())
        val event = objectMapper.readValue(startMessage, ParkingEvent::class.java)
        val clientResponse = SimpleParkClientResponse("IDDQD", SimpleParkParkingStatus.ACTIVE)

        `when`(
            parkClient.startParking(
                event.licensePlate!!,
                event.areaCode!!,
                event.startTime!!,
                event.endTime
            )
        ).thenReturn(clientResponse)
        kafkaTemplate.send("parking.events", "testKey", startMessage)
            .get(10, TimeUnit.SECONDS)

        verify(parkClient, Mockito.timeout(10000)).startParking(
            event.licensePlate!!,
            event.areaCode!!,
            event.startTime!!,
            event.endTime
        )
//        assertTrue(parkParkingJpaRepository.existsByInternalParkingId(event.parkingId))
    }
}
