package com.arrive.hometask.handler.impl

import com.arrive.hometask.client.SimpleParkClient
import com.arrive.hometask.client.SimpleParkParkingStatus
import com.arrive.hometask.handler.TypedParkingEventHandler
import com.arrive.hometask.listener.ParkingEvent
import com.arrive.hometask.listener.ParkingEventType
import com.arrive.hometask.service.SimpleParkParkingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class StopParkingEventHandler(
    override val eventType: ParkingEventType = ParkingEventType.PARKING_STOPPED,
    private val parkClient: SimpleParkClient,
    private val simpleParkParkingService: SimpleParkParkingService,
) : TypedParkingEventHandler {

    private val logger = LoggerFactory.getLogger(StopParkingEventHandler::class.java)

    override fun invoke(event: ParkingEvent) {
        val existingParking =
            requireNotNull(simpleParkParkingService.findByExternalId(event.parkingId)) { "Parking with ID ${event.parkingId} does not exist" }
        if (existingParking.status != SimpleParkParkingStatus.ACTIVE) {
            logger.warn("Parking with ID ${event.parkingId} is not active. Skipping event.")
            return
        }
        val externalParkingId =
            requireNotNull(existingParking.externalParkingId) { "externalParkingId in $existingParking cannot be null" }
        val endTime = requireNotNull(event.endTime) { "endTime in $event cannot be null" }
        if (endTime <= existingParking.startTime) {
            throw IllegalArgumentException("End time for parking $existingParking must be after current start time. Provided: $endTime, current: ${existingParking.startTime}")
        }

        val stopParkingResult = runCatching { parkClient.stopParking(externalParkingId, endTime).status }
        if (stopParkingResult.isFailure || stopParkingResult.getOrNull() == SimpleParkParkingStatus.FAILED) {
            simpleParkParkingService.save(
                existingParking.copy(
                    endTime = endTime,
                    status = SimpleParkParkingStatus.FAILED
                )
            )
            throw IllegalStateException(
                "Parking with ID ${event.parkingId} and external ID $externalParkingId failed to stop",
                stopParkingResult.exceptionOrNull()
            )
        }

        simpleParkParkingService.save(existingParking.copy(endTime = endTime, status = SimpleParkParkingStatus.STOPPED))
    }
}
