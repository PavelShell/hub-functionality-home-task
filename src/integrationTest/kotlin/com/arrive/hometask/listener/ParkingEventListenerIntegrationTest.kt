package com.arrive.hometask.listener

import com.arrive.hometask.client.model.SimpleParkParkingStatus
import com.arrive.hometask.config.IntegrationTestConfig
import com.arrive.hometask.config.KafkaProducerTestConfig
import com.arrive.hometask.db.SimpleParkParkingJpaRepository
import com.arrive.hometask.listener.model.ParkingEvent
import com.arrive.hometask.listener.model.ParkingEventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.junit5.WireMockExtension
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit

@SpringBootTest
@ActiveProfiles("integration-test")
@Import(KafkaProducerTestConfig::class)
class ParkingEventListenerIntegrationTest : IntegrationTestConfig() {

    companion object {

        @RegisterExtension
        val wireMock = WireMockExtension.newInstance()
            .options(com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort())
            .build()

        @JvmStatic
        @DynamicPropertySource
        fun overrideProperties(registry: DynamicPropertyRegistry) {
            registry.add("simple-park.api.base-url") { "${wireMock.baseUrl()}/api/v1" }
        }
    }

    @Autowired
    private lateinit var testKafkaTemplate: KafkaTemplate<String, Any>

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var repository: SimpleParkParkingJpaRepository

    @BeforeEach
    fun setUp() {
        repository.deleteAll()
        wireMock.resetAll()
    }

    @Test
    fun `should process parking started event end-to-end`() {
        // given
        val parkingId = "internal-123"
        val externalId = "external-999"
        val event = ParkingEvent(
            eventType = ParkingEventType.PARKING_STARTED,
            parkingId = parkingId,
            licensePlate = "ABC-123",
            areaCode = "ZONE-A",
            startTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )

        wireMock.stubFor(
            post(urlEqualTo("/api/v1/parking/start"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"parkingId": "$externalId", "status": "ACTIVE"}""")
                )
        )

        // when
        sendEvent(event)

        // then
        await {
            val parking = repository.findOneByInternalParkingId(parkingId)
            assertNotNull(parking)
            assertEquals(externalId, parking?.externalParkingId)
            assertEquals(SimpleParkParkingStatus.ACTIVE, parking?.status)
        }

        wireMock.verify(
            postRequestedFor(urlEqualTo("/api/v1/parking/start"))
                .withHeader("X-API-Key", equalTo("test-api-key-12345"))
        )
    }

    @Test
    fun `should process extend parking event end-to-end`() {
        // given
        val parkingId = "internal-123"
        val externalId = "external-999"
        val initialEndTime = Instant.now().plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS)
        val newEndTime = initialEndTime.plus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS)

        val startEvent = ParkingEvent(
            eventType = ParkingEventType.PARKING_STARTED,
            parkingId = parkingId,
            licensePlate = "ABC-123",
            areaCode = "ZONE-A",
            startTime = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS),
            endTime = initialEndTime
        )

        wireMock.stubFor(
            post(urlEqualTo("/api/v1/parking/start"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"parkingId": "$externalId", "status": "ACTIVE"}""")
                )
        )

        wireMock.stubFor(
            post(urlEqualTo("/api/v1/parking/$externalId/extend"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"parkingId": "$externalId", "status": "ACTIVE"}""")
                )
        )

        // when
        sendEvent(startEvent)
        await { assertNotNull(repository.findOneByInternalParkingId(parkingId)) }

        val extendEvent = ParkingEvent(
            eventType = ParkingEventType.PARKING_EXTENDED,
            parkingId = parkingId,
            endTime = newEndTime
        )
        sendEvent(extendEvent)

        // then
        await {
            val parking = repository.findOneByInternalParkingId(parkingId)
            assertEquals(newEndTime, parking?.endTime)
            assertEquals(SimpleParkParkingStatus.ACTIVE, parking?.status)
        }
    }

    @Test
    fun `should process stop parking event end-to-end`() {
        // given
        val parkingId = "internal-123"
        val externalId = "external-999"
        val stopTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)

        val startEvent = ParkingEvent(
            eventType = ParkingEventType.PARKING_STARTED,
            parkingId = parkingId,
            licensePlate = "ABC-123",
            areaCode = "ZONE-A",
            startTime = Instant.now().minus(1, ChronoUnit.HOURS).truncatedTo(ChronoUnit.SECONDS)
        )

        wireMock.stubFor(
            post(urlEqualTo("/api/v1/parking/start"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"parkingId": "$externalId", "status": "ACTIVE"}""")
                )
        )

        wireMock.stubFor(
            post(urlEqualTo("/api/v1/parking/$externalId/stop"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"parkingId": "$externalId", "status": "STOPPED"}""")
                )
        )

        // when
        sendEvent(startEvent)
        await { assertNotNull(repository.findOneByInternalParkingId(parkingId)) }

        val stopEvent = ParkingEvent(
            eventType = ParkingEventType.PARKING_STOPPED,
            parkingId = parkingId,
            endTime = stopTime
        )
        sendEvent(stopEvent)

        // then
        await {
            val parking = repository.findOneByInternalParkingId(parkingId)
            assertEquals(stopTime, parking?.endTime)
            assertEquals(SimpleParkParkingStatus.STOPPED, parking?.status)
        }
    }

    @Test
    fun `should handle api error during start parking`() {
        // given
        val parkingId = "internal-123"
        val event = ParkingEvent(
            eventType = ParkingEventType.PARKING_STARTED,
            parkingId = parkingId,
            licensePlate = "ABC-123",
            areaCode = "ZONE-A",
            startTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )

        wireMock.stubFor(
            post(urlEqualTo("/api/v1/parking/start"))
                .willReturn(
                    aResponse()
                        .withStatus(500)
                )
        )

        // when
        sendEvent(event)

        // then
        await {
            val parking = repository.findOneByInternalParkingId(parkingId)
            assertNull(parking)
        }
    }

    @Test
    fun `should be idempotent when receiving duplicate parking started event`() {
        // given
        val parkingId = "idempotent-1"
        val externalId = "ext-idempotent-1"
        val event = ParkingEvent(
            eventType = ParkingEventType.PARKING_STARTED,
            parkingId = parkingId,
            licensePlate = "ABC-123",
            areaCode = "ZONE-A",
            startTime = Instant.now().truncatedTo(ChronoUnit.SECONDS)
        )

        wireMock.stubFor(
            post(urlEqualTo("/api/v1/parking/start"))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("""{"parkingId": "$externalId", "status": "ACTIVE"}""")
                )
        )

        // when
        sendEvent(event)
        await { assertNotNull(repository.findOneByInternalParkingId(parkingId)) }

        sendEvent(event) // duplicate

        // then
        Thread.sleep(1000) // Wait to see if anything bad happens (like extra API calls or DB errors)

        val count = repository.findAll().count { it.internalParkingId == parkingId }
        assertEquals(1, count)

        wireMock.verify(1, postRequestedFor(urlEqualTo("/api/v1/parking/start")))
    }

    private fun sendEvent(event: ParkingEvent) {
        val message = objectMapper.writeValueAsString(event)
        testKafkaTemplate.send("parking.events", event.parkingId, message).get(10, TimeUnit.SECONDS)
    }

    private fun await(condition: () -> Unit) {
        val start = System.currentTimeMillis()
        var lastError: Throwable? = null
        while (System.currentTimeMillis() - start < 10000) {
            try {
                condition()
                return
            } catch (e: Throwable) {
                lastError = e
                Thread.sleep(200)
            }
        }
        throw lastError ?: RuntimeException("Timeout waiting for condition")
    }
}
