package com.arrive.hometask.handler.impl

import com.arrive.hometask.client.SimpleParkClient
import com.arrive.hometask.client.model.SimpleParkClientResponse
import com.arrive.hometask.client.model.SimpleParkParkingStatus
import com.arrive.hometask.db.SimpleParkParking
import com.arrive.hometask.listener.model.ParkingEvent
import com.arrive.hometask.listener.model.ParkingEventType
import com.arrive.hometask.service.SimpleParkParkingService
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.junit5.MockKExtension
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
class StartParkingEventHandlerTest {

    @MockK
    private lateinit var parkClient: SimpleParkClient

    @MockK
    private lateinit var simpleParkParkingService: SimpleParkParkingService

    private lateinit var handler: StartParkingEventHandler

    @BeforeEach
    fun setUp() {
        handler = StartParkingEventHandler(
            parkClient = parkClient,
            simpleParkParkingService = simpleParkParkingService,
        )
    }

    @Test
    fun `should save new parking info when event is valid and parking doesn't exist`() {
        // given
        val event = createEvent()
        val clientResponse = SimpleParkClientResponse("EXTERNAL_ID", SimpleParkParkingStatus.ACTIVE)

        every { simpleParkParkingService.findByInternalParkingId(event.parkingId) } returns null
        every {
            parkClient.startParking(
                event.licensePlate!!,
                event.areaCode!!,
                event.startTime!!,
                event.endTime
            )
        } returns clientResponse
        every { simpleParkParkingService.save(any()) } returns createParking(event)

        // when
        handler.invoke(event)

        // then
        val slot = slot<SimpleParkParking>()
        verify { simpleParkParkingService.save(capture(slot)) }
        val saved = slot.captured
        assertEquals(event.parkingId, saved.internalParkingId)
        assertEquals(event.licensePlate, saved.licensePlate)
        assertEquals(event.areaCode, saved.areaCode)
        assertEquals(event.startTime, saved.startTime)
        assertEquals(event.endTime, saved.endTime)
        assertEquals(clientResponse.status, saved.status)
        assertEquals(clientResponse.parkingId, saved.externalParkingId)
    }

    @Test
    fun `should skip processing when parking already exists`() {
        // given
        val event = createEvent()
        val existingParking = createParking(event)

        every { simpleParkParkingService.findByInternalParkingId(event.parkingId) } returns existingParking

        // when
        handler.invoke(event)

        // then
        verify(exactly = 0) {
            parkClient.startParking(any(), any(), any(), any())
            simpleParkParkingService.save(any())
        }
    }

    @Test
    fun `should throw exception when license plate is missing`() {
        // given
        val event = createEvent(licensePlate = null)

        every { simpleParkParkingService.findByInternalParkingId(event.parkingId) } returns null

        // when & then
        assertThrows<IllegalArgumentException> {
            handler.invoke(event)
        }
    }

    @Test
    fun `should throw exception when client returns non-active status`() {
        // given
        val event = createEvent()
        val clientResponse = SimpleParkClientResponse("EXTERNAL_ID", SimpleParkParkingStatus.STOPPED)

        every { simpleParkParkingService.findByInternalParkingId(event.parkingId) } returns null
        every {
            parkClient.startParking(
                event.licensePlate!!,
                event.areaCode!!,
                event.startTime!!,
                event.endTime
            )
        } returns clientResponse

        // when & then
        assertThrows<IllegalStateException> {
            handler.invoke(event)
        }
        verify(exactly = 0) { simpleParkParkingService.save(any()) }
    }

    @Test
    fun `should stop parking when database save fails`() {
        // given
        val event = createEvent()
        val clientResponse = SimpleParkClientResponse("EXTERNAL_ID", SimpleParkParkingStatus.ACTIVE)

        every { simpleParkParkingService.findByInternalParkingId(event.parkingId) } returns null
        every {
            parkClient.startParking(
                event.licensePlate!!,
                event.areaCode!!,
                event.startTime!!,
                event.endTime
            )
        } returns clientResponse
        every { simpleParkParkingService.save(any()) } throws RuntimeException("DB Error")
        // we need to return something for stopParking call
        every { parkClient.stopParking(any(), any()) } returns SimpleParkClientResponse(
            "EXTERNAL_ID",
            SimpleParkParkingStatus.STOPPED
        )

        // when & then
        assertThrows<RuntimeException> {
            handler.invoke(event)
        }
        verify { parkClient.stopParking(any(), any()) }
    }

    @Test
    fun `should handle stop parking failure when database save fails`() {
        // given
        val event = createEvent()
        val clientResponse = SimpleParkClientResponse("EXTERNAL_ID", SimpleParkParkingStatus.ACTIVE)

        every { simpleParkParkingService.findByInternalParkingId(event.parkingId) } returns null
        every {
            parkClient.startParking(
                event.licensePlate!!,
                event.areaCode!!,
                event.startTime!!,
                event.endTime
            )
        } returns clientResponse
        every { simpleParkParkingService.save(any()) } throws RuntimeException("DB Error")
        every { parkClient.stopParking(any(), any()) } throws RuntimeException("Stop Error")

        // when & then
        val exception = assertThrows<RuntimeException> {
            handler.invoke(event)
        }
        assertEquals("DB Error", exception.message)
        assert(exception.suppressedExceptions.any { it.message == "Stop Error" })
    }

    private fun createParking(event: ParkingEvent) = SimpleParkParking(
        areaCode = event.areaCode!!,
        internalParkingId = event.parkingId,
        externalParkingId = "EXTERNAL_ID",
        licensePlate = event.licensePlate!!,
        startTime = event.startTime!!,
        endTime = event.endTime,
        status = SimpleParkParkingStatus.ACTIVE
    )

    private fun createEvent(
        licensePlate: String? = "PLATE",
        areaCode: String? = "AREA",
        startTime: Instant? = Instant.now()
    ) = ParkingEvent(
        eventType = ParkingEventType.PARKING_STARTED,
        parkingId = "INTERNAL_ID",
        licensePlate = licensePlate,
        areaCode = areaCode,
        startTime = startTime,
        endTime = startTime?.plusSeconds(3600),
        priceAmount = 10.0,
        currency = "EUR"
    )
}
