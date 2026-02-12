package com.arrive.hometask.handler

import com.arrive.hometask.client.SimpleParkClient
import com.arrive.hometask.client.SimpleParkClientResponse
import com.arrive.hometask.client.SimpleParkParkingStatus
import com.arrive.hometask.db.SimpleParkParking
import com.arrive.hometask.handler.impl.StartParkingEventHandler
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
    fun `should save new parking info`() {
        val event = ParkingEvent(
            ParkingEventType.PARKING_STARTED,
            "IDDQD",
            "license-plate",
            "area-code",
            Instant.now(),
            Instant.now().plusSeconds(10),
            10.0,
            "EUR"
        )
        val clientResponse = SimpleParkClientResponse("IDDQD", SimpleParkParkingStatus.ACTIVE)

        `when`(
            parkClient.startParking(
                event.licensePlate!!,
                event.areaCode!!,
                event.startTime!!,
                event.endTime
            )
        ).thenReturn(clientResponse)
        `when`(simpleParkParkingService.findByExternalId(event.parkingId)).thenReturn(null)
        handler.invoke(event)

        verify(parkClient).startParking(
            event.licensePlate!!,
            event.areaCode!!,
            event.startTime!!,
            event.endTime
        )
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
}
