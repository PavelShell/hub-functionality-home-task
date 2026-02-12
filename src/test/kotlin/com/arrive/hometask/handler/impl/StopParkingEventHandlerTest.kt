package com.arrive.hometask.handler.impl

import com.arrive.hometask.client.SimpleParkClient
import com.arrive.hometask.client.SimpleParkClientResponse
import com.arrive.hometask.client.SimpleParkParkingStatus
import com.arrive.hometask.db.SimpleParkParking
import com.arrive.hometask.listener.ParkingEvent
import com.arrive.hometask.listener.ParkingEventType
import com.arrive.hometask.service.SimpleParkParkingService
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.junit.jupiter.MockitoExtension
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class StopParkingEventHandlerTest {

    @Mock
    private lateinit var parkClient: SimpleParkClient

    @Mock
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

        `when`(simpleParkParkingService.findByExternalId(parkingId)).thenReturn(existingParking)
        `when`(parkClient.stopParking(externalId, endTime)).thenReturn(clientResponse)

        // when
        handler.invoke(event)

        // then
        verify(simpleParkParkingService).save(
            existingParking.copy(endTime = endTime, status = SimpleParkParkingStatus.STOPPED)
        )
    }

    @Test
    fun `should throw exception when parking does not exist`() {
        // given
        val event = createEvent()
        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(null)

        // when & then
        assertThrows<IllegalArgumentException> {
            handler.invoke(event)
        }
        verify(parkClient, never()).stopParking(anyString(), anyInstance(Instant::class.java))
    }

    @Test
    fun `should skip processing when parking is not active`() {
        // given
        val event = createEvent()
        val existingParking = createParking(event.parkingId).apply { status = SimpleParkParkingStatus.STOPPED }
        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(existingParking)

        // when
        handler.invoke(event)

        // then
        verify(parkClient, never()).stopParking(anyString(), anyInstance(Instant::class.java))
        verify(simpleParkParkingService, never()).save(anyInstance(SimpleParkParking::class.java))
    }

    @Test
    fun `should throw exception when end time is before start time`() {
        // given
        val startTime = Instant.now()
        val endTime = startTime.minusSeconds(60)
        val event = createEvent(endTime = endTime)
        val existingParking = createParking(event.parkingId, startTime = startTime)

        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(existingParking)

        // when & then
        assertThrows<IllegalArgumentException> {
            handler.invoke(event)
        }
        verify(parkClient, never()).stopParking(anyString(), anyInstance(Instant::class.java))
    }

    @Test
    fun `should save FAILED status and throw exception when client fails`() {
        // given
        val parkingId = "parking-123"
        val externalId = "ext-456"
        val endTime = Instant.now()
        val event = createEvent(parkingId, endTime)
        val existingParking = createParking(parkingId, externalId)

        `when`(simpleParkParkingService.findByExternalId(parkingId)).thenReturn(existingParking)
        `when`(parkClient.stopParking(externalId, endTime)).thenThrow(RuntimeException("API Error"))

        // when & then
        assertThrows<IllegalStateException> {
            handler.invoke(event)
        }
        verify(simpleParkParkingService).save(
            existingParking.copy(endTime = endTime, status = SimpleParkParkingStatus.FAILED)
        )
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

        `when`(simpleParkParkingService.findByExternalId(parkingId)).thenReturn(existingParking)
        `when`(parkClient.stopParking(externalId, endTime)).thenReturn(clientResponse)

        // when & then
        assertThrows<IllegalStateException> {
            handler.invoke(event)
        }
        verify(simpleParkParkingService).save(
            existingParking.copy(endTime = endTime, status = SimpleParkParkingStatus.FAILED)
        )
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

    private fun <T> anyInstance(type: Class<T>): T = any(type)
}
