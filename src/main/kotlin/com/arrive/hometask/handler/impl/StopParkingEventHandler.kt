package com.arrive.hometask.handler.impl

import com.arrive.hometask.client.SimpleParkClient
import com.arrive.hometask.client.model.SimpleParkParkingStatus
import com.arrive.hometask.handler.TypedParkingEventHandler
import com.arrive.hometask.listener.model.ParkingEvent
import com.arrive.hometask.listener.model.ParkingEventType
import com.arrive.hometask.service.SimpleParkParkingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

/**
 * Handler for [ParkingEventType.PARKING_STOPPED] events.
 * Terminates an existing active parking session in both the external system and local database.
 * If the external system fails, the local record is marked as FAILED.
 */
@Component
class StopParkingEventHandler(
    override val eventType: ParkingEventType = ParkingEventType.PARKING_STOPPED,
    private val parkClient: SimpleParkClient,
    private val simpleParkParkingService: SimpleParkParkingService,
) : TypedParkingEventHandler {

    private val logger = LoggerFactory.getLogger(StopParkingEventHandler::class.java)

    /**
     * Handles the stop parking event.
     * Skips processing if the parking is already not ACTIVE.
     *
     * @param event The parking stopped event.
     * @throws IllegalArgumentException if the parking session is not found, or the end time is invalid.
     * @throws IllegalStateException if the external API call fails.
     */
    override fun invoke(event: ParkingEvent) {
        val existingParking =
            requireNotNull(simpleParkParkingService.findByInternalParkingId(event.parkingId)) { "Parking with ID ${event.parkingId} does not exist" }
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
            existingParking.endTime = endTime
            existingParking.status = SimpleParkParkingStatus.FAILED
            simpleParkParkingService.save(existingParking)
            throw IllegalStateException(
                "Parking with ID ${event.parkingId} and external ID $externalParkingId failed to stop",
                stopParkingResult.exceptionOrNull()
            )
        }

        existingParking.endTime = endTime
        existingParking.status = SimpleParkParkingStatus.STOPPED
        simpleParkParkingService.save(existingParking)
    }
}
