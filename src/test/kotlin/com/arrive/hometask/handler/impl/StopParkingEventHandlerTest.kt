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
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant

@ExtendWith(MockKExtension::class)
class StopParkingEventHandlerTest {

    @MockK
    private lateinit var parkClient: SimpleParkClient

    @MockK
    private lateinit var simpleParkParkingService: SimpleParkParkingService

    private lateinit var handler: StopParkingEventHandler

    @BeforeEach
    fun setUp() {
        handler = StopParkingEventHandler(
            parkClient = parkClient,
            simpleParkParkingService = simpleParkParkingService,
        )
    }

    @Test
    fun `should stop parking when event is valid and parking is active`() {
        // given
        val parkingId = "parking-123"
        val externalId = "ext-456"
        val endTime = Instant.now()
        val event = createEvent(parkingId, endTime)
        val existingParking = createParking(parkingId, externalId)
        val clientResponse = SimpleParkClientResponse(externalId, SimpleParkParkingStatus.STOPPED)

        every { simpleParkParkingService.findByInternalParkingId(parkingId) } returns existingParking
        every { parkClient.stopParking(externalId, endTime) } returns clientResponse
        every { simpleParkParkingService.save(any()) } returns existingParking

        // when
        handler.invoke(event)

        // then
        assertEquals(endTime, existingParking.endTime)
        assertEquals(SimpleParkParkingStatus.STOPPED, existingParking.status)
        verify { simpleParkParkingService.save(any()) }
    }

    @Test
    fun `should throw exception when parking does not exist`() {
        // given
        val event = createEvent()
        every { simpleParkParkingService.findByInternalParkingId(event.parkingId) } returns null

        // when & then
        assertThrows<IllegalArgumentException> {
            handler.invoke(event)
        }
        verify(exactly = 0) { parkClient.stopParking(any(), any()) }
    }

    @Test
    fun `should skip processing when parking is not active`() {
        // given
        val event = createEvent()
        val existingParking = createParking(event.parkingId).apply { status = SimpleParkParkingStatus.STOPPED }
        every { simpleParkParkingService.findByInternalParkingId(event.parkingId) } returns existingParking

        // when
        handler.invoke(event)

        // then
        verify(exactly = 0) { parkClient.stopParking(any(), any()) }
        verify(exactly = 0) { simpleParkParkingService.save(any()) }
    }

    @Test
    fun `should throw exception when end time is before start time`() {
        // given
        val startTime = Instant.now()
        val endTime = startTime.minusSeconds(60)
        val event = createEvent(endTime = endTime)
        val existingParking = createParking(event.parkingId, startTime = startTime)

        every { simpleParkParkingService.findByInternalParkingId(event.parkingId) } returns existingParking

        // when & then
        assertThrows<IllegalArgumentException> {
            handler.invoke(event)
        }
        verify(exactly = 0) { parkClient.stopParking(any(), any()) }
    }

    @Test
    fun `should save FAILED status and throw exception when client fails`() {
        // given
        val parkingId = "parking-123"
        val externalId = "ext-456"
        val endTime = Instant.now()
        val event = createEvent(parkingId, endTime)
        val existingParking = createParking(parkingId, externalId)

        every { simpleParkParkingService.findByInternalParkingId(parkingId) } returns existingParking
        every { parkClient.stopParking(externalId, endTime) } throws RuntimeException("API Error")
        every { simpleParkParkingService.save(any()) } returns existingParking

        // when & then
        assertThrows<IllegalStateException> {
            handler.invoke(event)
        }
        assertEquals(endTime, existingParking.endTime)
        assertEquals(SimpleParkParkingStatus.FAILED, existingParking.status)
        verify { simpleParkParkingService.save(any()) }
    }

    @Test
    fun `should save FAILED status and throw exception when client returns FAILED status`() {
        // given
        val parkingId = "parking-123"
        val externalId = "ext-456"
        val endTime = Instant.now()
        val event = createEvent(parkingId, endTime)
        val existingParking = createParking(parkingId, externalId)
        val clientResponse = SimpleParkClientResponse(externalId, SimpleParkParkingStatus.FAILED)

        every { simpleParkParkingService.findByInternalParkingId(parkingId) } returns existingParking
        every { parkClient.stopParking(externalId, endTime) } returns clientResponse
        every { simpleParkParkingService.save(any()) } returns existingParking

        // when & then
        assertThrows<IllegalStateException> {
            handler.invoke(event)
        }
        assertEquals(endTime, existingParking.endTime)
        assertEquals(SimpleParkParkingStatus.FAILED, existingParking.status)
        verify { simpleParkParkingService.save(any()) }
    }

    private fun createEvent(
        parkingId: String = "parking-123",
        endTime: Instant = Instant.now()
    ) = ParkingEvent(
        eventType = ParkingEventType.PARKING_STOPPED,
        parkingId = parkingId,
        endTime = endTime
    )

    private fun createParking(
        parkingId: String,
        externalId: String = "ext-456",
        startTime: Instant = Instant.now().minusSeconds(3600)
    ) = SimpleParkParking(
        areaCode = "AREA-1",
        internalParkingId = parkingId,
        externalParkingId = externalId,
        licensePlate = "ABC-123",
        startTime = startTime,
        status = SimpleParkParkingStatus.ACTIVE
    )
}
