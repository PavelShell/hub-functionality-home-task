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
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import org.mockito.junit.jupiter.MockitoExtension
import org.junit.jupiter.api.assertThrows
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.any
import org.mockito.Mockito.never
import java.time.Instant

@ExtendWith(MockitoExtension::class)
class StartParkingEventHandlerTest {

    @Mock
    private lateinit var parkClient: SimpleParkClient

    @Mock
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

        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(null)
        `when`(
            parkClient.startParking(
                event.licensePlate!!,
                event.areaCode!!,
                event.startTime!!,
                event.endTime
            )
        ).thenReturn(clientResponse)

        // when
        handler.invoke(event)

        // then
        verify(simpleParkParkingService).save(
            SimpleParkParking(
                areaCode = event.areaCode!!,
                internalParkingId = event.parkingId,
                externalParkingId = clientResponse.parkingId,
                licensePlate = event.licensePlate!!,
                startTime = event.startTime!!,
                endTime = event.endTime,
                status = clientResponse.status
            )
        )
    }

    @Test
    fun `should skip processing when parking already exists`() {
        // given
        val event = createEvent()
        val existingParking = createParking(event)

        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(existingParking)

        // when
        handler.invoke(event)

        // then
        verify(parkClient, never()).startParking(anyString(), anyString(), anyInstance(Instant::class.java), anyInstance(Instant::class.java))
        verify(simpleParkParkingService, never()).save(anyInstance(SimpleParkParking::class.java))
    }

    @Test
    fun `should throw exception when license plate is missing`() {
        // given
        val event = createEvent(licensePlate = null)

        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(null)

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

        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(null)
        `when`(
            parkClient.startParking(
                event.licensePlate!!,
                event.areaCode!!,
                event.startTime!!,
                event.endTime
            )
        ).thenReturn(clientResponse)

        // when & then
        assertThrows<IllegalStateException> {
            handler.invoke(event)
        }
        verify(simpleParkParkingService, never()).save(anyInstance(SimpleParkParking::class.java))
    }

    @Test
    fun `should stop parking when database save fails`() {
        // given
        val event = createEvent()
        val clientResponse = SimpleParkClientResponse("EXTERNAL_ID", SimpleParkParkingStatus.ACTIVE)

        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(null)
        `when`(
            parkClient.startParking(
                event.licensePlate!!,
                event.areaCode!!,
                event.startTime!!,
                event.endTime
            )
        ).thenReturn(clientResponse)
        `when`(simpleParkParkingService.save(anyInstance(SimpleParkParking::class.java))).thenThrow(RuntimeException("DB Error"))
        // we need to return something for stopParking call
        `when`(parkClient.stopParking(anyString(), anyInstance(Instant::class.java))).thenReturn(SimpleParkClientResponse("EXTERNAL_ID", SimpleParkParkingStatus.STOPPED))

        // when & then
        assertThrows<RuntimeException> {
            handler.invoke(event)
        }
        verify(parkClient).stopParking(anyString(), anyInstance(Instant::class.java))
    }

    @Test
    fun `should handle stop parking failure when database save fails`() {
        // given
        val event = createEvent()
        val clientResponse = SimpleParkClientResponse("EXTERNAL_ID", SimpleParkParkingStatus.ACTIVE)

        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(null)
        `when`(
            parkClient.startParking(
                event.licensePlate!!,
                event.areaCode!!,
                event.startTime!!,
                event.endTime
            )
        ).thenReturn(clientResponse)
        `when`(simpleParkParkingService.save(anyInstance(SimpleParkParking::class.java))).thenThrow(RuntimeException("DB Error"))
        `when`(parkClient.stopParking(anyString(), anyInstance(Instant::class.java))).thenThrow(RuntimeException("Stop Error"))

        // when & then
        val exception = assertThrows<RuntimeException> {
            handler.invoke(event)
        }
        assert(exception.message == "DB Error")
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

    private fun <T> anyInstance(type: Class<T>): T = any(type)
}
