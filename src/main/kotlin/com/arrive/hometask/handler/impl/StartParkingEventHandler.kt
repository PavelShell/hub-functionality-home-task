package com.arrive.hometask.handler.impl

import com.arrive.hometask.client.SimpleParkClient
import com.arrive.hometask.client.model.SimpleParkParkingStatus
import com.arrive.hometask.db.SimpleParkParking
import com.arrive.hometask.handler.TypedParkingEventHandler
import com.arrive.hometask.listener.model.ParkingEvent
import com.arrive.hometask.listener.model.ParkingEventType
import com.arrive.hometask.service.SimpleParkParkingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.Instant

/**
 * Handler for [ParkingEventType.PARKING_STARTED] events.
 * Registers a new parking session in the external system and saves it to the local database.
 * Implements a compensation pattern: if local save fails, it attempts to stop the parking in the external system.
 */
@Component
class StartParkingEventHandler(
    override val eventType: ParkingEventType = ParkingEventType.PARKING_STARTED,
    private val parkClient: SimpleParkClient,
    private val simpleParkParkingService: SimpleParkParkingService,
) : TypedParkingEventHandler {

    private val logger = LoggerFactory.getLogger(StartParkingEventHandler::class.java)

    /**
     * Handles the [ParkingEventType.PARKING_STARTED] parking event.
     * Skips processing if a parking with the same internal ID already exists (idempotency).
     *
     * @param event The parking started event.
     * @throws IllegalArgumentException if required fields are missing in the event.
     * @throws IllegalStateException if the external system returns a non-ACTIVE status.
     */
    override fun invoke(event: ParkingEvent) {
        val existingParking = simpleParkParkingService.findByInternalParkingId(event.parkingId)
        if (existingParking != null) {
            logger.warn("Parking with ID ${event.parkingId} already exists. Skipping event.")
            return
        }
        val licensePlate = requireNotNull(event.licensePlate) { "licensePlate in $event cannot be null" }
        val areaCode = requireNotNull(event.areaCode) { "areaCode in $event cannot be null" }
        val startTime = requireNotNull(event.startTime) { "startTime in $event cannot be null" }

        val (externalParkingId, status) = parkClient.startParking(licensePlate, areaCode, startTime, event.endTime)

        when (status) {
            SimpleParkParkingStatus.ACTIVE -> saveNewParking(
                areaCode = areaCode,
                internalParkingId = event.parkingId,
                externalParkingId = externalParkingId,
                licensePlate = licensePlate,
                startTime = startTime,
                endTime = event.endTime,
            )

            else -> throw IllegalStateException("Parking with ID ${event.parkingId} and external ID $externalParkingId failed to start. Actual status: $status")
        }
    }

    private fun saveNewParking(
        areaCode: String,
        internalParkingId: String,
        externalParkingId: String,
        licensePlate: String,
        startTime: Instant,
        endTime: Instant?,
    ) = try {
        simpleParkParkingService.save(
            SimpleParkParking(
                areaCode = areaCode,
                internalParkingId = internalParkingId,
                externalParkingId = externalParkingId,
                licensePlate = licensePlate,
                startTime = startTime,
                endTime = endTime,
                status = SimpleParkParkingStatus.ACTIVE
            )
        )
    } catch (e: Exception) {
        runCatching { parkClient.stopParking(externalParkingId, Instant.now()) }
            .onFailure {
                logger.error("Failed to stop parking with external ID $externalParkingId", it)
                e.addSuppressed(it)
            }
            .onSuccess {
                if (it.status != SimpleParkParkingStatus.STOPPED) {
                    logger.warn("Failed to stop parking with external ID $externalParkingId. Actual status: ${it.status}")
                }
            }
        throw e
    }
}
