package com.arrive.hometask.listener

import com.arrive.hometask.config.DbAwareTestConfig
import com.arrive.hometask.handler.ParkingEventHandler
import com.arrive.hometask.handler.ParkingEventHandlerFactory
import com.arrive.hometask.listener.model.ParkingEvent
import com.arrive.hometask.listener.model.ParkingEventType
import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.*
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.test.context.EmbeddedKafka
import org.springframework.test.context.ActiveProfiles
import java.util.concurrent.TimeUnit

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
class ParkingEventListenerTest : DbAwareTestConfig() {

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @Autowired
    private lateinit var kafkaTemplate: KafkaTemplate<String?, String?>

    @MockkBean
    private lateinit var handlersFactory: ParkingEventHandlerFactory

    @Test
    fun `should process parking event when message is received`() {
        // given
        val event = ParkingEvent(
            eventType = ParkingEventType.PARKING_STARTED,
            parkingId = "parking-123",
            licensePlate = "ABC-123",
            areaCode = "AREA-1"
        )
        val eventMessage = objectMapper.writeValueAsString(event)
        val handlerMock = mockk<ParkingEventHandler>()

        every { handlersFactory.getHandler(event.eventType) } returns handlerMock
        every { handlerMock.invoke(any()) } just runs

        // when
        kafkaTemplate.send("parking.events", "testKey", eventMessage).get(10, TimeUnit.SECONDS)

        // then
        verify(timeout = 10000) {
            handlersFactory.getHandler(event.eventType)
            handlerMock.invoke(event)
        }
    }
}
