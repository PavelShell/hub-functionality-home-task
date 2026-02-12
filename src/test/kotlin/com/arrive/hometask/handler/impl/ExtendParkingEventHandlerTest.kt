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
class ExtendParkingEventHandlerTest {

    @Mock
    private lateinit var parkClient: SimpleParkClient

    @Mock
    private lateinit var simpleParkParkingService: SimpleParkParkingService

    private lateinit var handler: ExtendParkingEventHandler

    @BeforeEach
    fun setUp() {
        handler = ExtendParkingEventHandler(
            parkClient = parkClient,
            simpleParkParkingService = simpleParkParkingService,
        )
    }

    @Test
    fun `should extend parking when event is valid and parking is active`() {
        // given
        val parkingId = "parking-123"
        val externalId = "ext-456"
        val initialEndTime = Instant.now().plusSeconds(3600)
        val newEndTime = initialEndTime.plusSeconds(3600)

        val event = createEvent(parkingId, newEndTime)
        val existingParking = createParking(parkingId, externalId, initialEndTime)
        val clientResponse = SimpleParkClientResponse(externalId, SimpleParkParkingStatus.ACTIVE)

        `when`(simpleParkParkingService.findByExternalId(parkingId)).thenReturn(existingParking)
        `when`(parkClient.extendParking(externalId, newEndTime)).thenReturn(clientResponse)

        // when
        handler.invoke(event)

        // then
        verify(simpleParkParkingService).save(
            existingParking.copy(endTime = newEndTime)
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
        verify(parkClient, never()).extendParking(anyString(), anyInstance(Instant::class.java))
    }

    @Test
    fun `should throw exception when parking is not active`() {
        // given
        val event = createEvent()
        val existingParking = createParking(event.parkingId).apply { status = SimpleParkParkingStatus.STOPPED }
        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(existingParking)

        // when & then
        assertThrows<IllegalStateException> {
            handler.invoke(event)
        }
        verify(parkClient, never()).extendParking(anyString(), anyInstance(Instant::class.java))
    }

    @Test
    fun `should throw exception when new end time is before current end time`() {
        // given
        val initialEndTime = Instant.now().plusSeconds(3600)
        val newEndTime = initialEndTime.minusSeconds(600)
        val event = createEvent(endTime = newEndTime)
        val existingParking = createParking(event.parkingId, endTime = initialEndTime)

        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(existingParking)

        // when & then
        assertThrows<IllegalArgumentException> {
            handler.invoke(event)
        }
        verify(parkClient, never()).extendParking(anyString(), anyInstance(Instant::class.java))
    }

    @Test
    fun `should throw exception when client returns non-active status`() {
        // given
        val event = createEvent()
        val existingParking = createParking(event.parkingId)
        val clientResponse = SimpleParkClientResponse("ext-id", SimpleParkParkingStatus.FAILED)

        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(existingParking)
        `when`(parkClient.extendParking(anyString(), anyInstance(Instant::class.java))).thenReturn(clientResponse)

        // when & then
        assertThrows<IllegalStateException> {
            handler.invoke(event)
        }
        verify(simpleParkParkingService, never()).save(anyInstance(SimpleParkParking::class.java))
    }

    private fun createEvent(
        parkingId: String = "parking-123",
        endTime: Instant = Instant.now().plusSeconds(7200)
    ) = ParkingEvent(
        eventType = ParkingEventType.PARKING_EXTENDED,
        parkingId = parkingId,
        endTime = endTime
    )

    private fun createParking(
        parkingId: String,
        externalId: String = "ext-456",
        endTime: Instant = Instant.now().plusSeconds(3600)
    ) = SimpleParkParking(
        areaCode = "AREA-1",
        internalParkingId = parkingId,
        externalParkingId = externalId,
        licensePlate = "ABC-123",
        startTime = Instant.now().minusSeconds(3600),
        endTime = endTime,
        status = SimpleParkParkingStatus.ACTIVE
    )

    private fun <T> anyInstance(type: Class<T>): T = any(type)
}
