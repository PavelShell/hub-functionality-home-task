package com.arrive.hometask.handler.impl

import com.arrive.hometask.client.SimpleParkClient
import com.arrive.hometask.client.model.SimpleParkParkingStatus
import com.arrive.hometask.handler.TypedParkingEventHandler
import com.arrive.hometask.listener.model.ParkingEvent
import com.arrive.hometask.listener.model.ParkingEventType
import com.arrive.hometask.service.SimpleParkParkingService
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class ExtendParkingEventHandler(
    override val eventType: ParkingEventType = ParkingEventType.PARKING_EXTENDED,
    private val parkClient: SimpleParkClient,
    private val simpleParkParkingService: SimpleParkParkingService,
) : TypedParkingEventHandler {

    private val logger = LoggerFactory.getLogger(ExtendParkingEventHandler::class.java)

    override fun invoke(event: ParkingEvent) {
        val existingParking =
            requireNotNull(simpleParkParkingService.findByInternalParkingId(event.parkingId)) { "Parking with ID ${event.parkingId} does not exist" }
        val newEndTime = requireNotNull(event.endTime) { "endTime in $event cannot be null" }
        if (newEndTime == existingParking.endTime) {
            logger.warn("Parking with ID ${event.parkingId} already has the endTime $newEndTime. Skipping event.")
            return
        }
        if (existingParking.endTime != null && newEndTime < existingParking.endTime) {
            throw IllegalArgumentException("New end time for parking $existingParking must be after current end time. Provided: $newEndTime, current: ${existingParking.endTime}")
        }
        if (existingParking.status != SimpleParkParkingStatus.ACTIVE) {
            throw IllegalStateException("Parking with ID ${event.parkingId} is not active. Actual status: ${existingParking.status}")
        }
        val externalParkingId =
            requireNotNull(existingParking.externalParkingId) { "externalParkingId in $existingParking cannot be null" }

        val (_, status) = parkClient.extendParking(externalParkingId, newEndTime)

        when (status) {
            SimpleParkParkingStatus.ACTIVE -> {
                existingParking.endTime = newEndTime
                simpleParkParkingService.save(existingParking)
            }
            // todo: should we set existing parking status to FAILED?
            else -> throw IllegalStateException("Parking with ID ${event.parkingId} and external ID $externalParkingId failed to extend. Actual status: $status")
        }
    }
}
