package com.arrive.hometask.listener

import com.arrive.hometask.client.SimpleParkClient
import com.arrive.hometask.client.SimpleParkParkingStatus
import com.arrive.hometask.db.SimpleParkParking
import com.arrive.hometask.db.SimpleParkParkingJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

/**
 * TODO: Implement Kafka listener for parking events
 *
 * Requirements:
 * - Listen to topic: "parking.events"
 * - Process all event types (PARKING_STARTED, PARKING_EXTENDED, PARKING_STOPPED)
 * - Handle errors gracefully (don't crash the consumer)
 * - Use manual acknowledgment for offset management
 *
 * Hint: Use @KafkaListener annotation with proper configuration
 */
@Component
class ParkingEventListener(
    private val parkClient: SimpleParkClient,
    private val parkParkingJpaRepository: SimpleParkParkingJpaRepository,
) {

    private val logger = LoggerFactory.getLogger(ParkingEventListener::class.java)

    // Transactions?
    @KafkaListener(topics = ["parking.events"], errorHandler = "listenerErrorHandler")
    fun processMessage(event: ParkingEvent, ack: Acknowledgment) {
        logger.info("Received event: $event")
        when (event.eventType) {
            ParkingEventType.PARKING_STARTED -> handleParkingStarted(event)
            ParkingEventType.PARKING_STOPPED -> handleParkingStopped(event)
            ParkingEventType.PARKING_EXTENDED -> handleParkingExtended(event)
        }
        ack.acknowledge()
    }

    private fun handleParkingStopped(event: ParkingEvent) {
        TODO("Not yet implemented")
    }

    private fun handleParkingExtended(event: ParkingEvent) {
        TODO("Not yet implemented")
    }

    private fun handleParkingStarted(event: ParkingEvent) {
        if (parkParkingJpaRepository.existsByInternalParkingId(event.parkingId)) {
            throw IllegalStateException("Parking with ID ${event.parkingId} already exists")
        }
        val licensePlate = requireNotNull(event.licensePlate) { "licensePlate in $event cannot be null" }
        val areaCode = requireNotNull(event.areaCode) { "areaCode in $event cannot be null" }
        val startTime = requireNotNull(event.startTime) { "startTime in $event cannot be null" }
        val (externalParkingId, status) = parkClient.startParking(licensePlate, areaCode, startTime, event.endTime)
        if (status != SimpleParkParkingStatus.ACTIVE) {
            throw IllegalStateException("Parking with ID ${event.parkingId} and external ID $externalParkingId failed to start. Actual status: $status")
        }
        parkParkingJpaRepository.save(
            SimpleParkParking(
                areaCode = areaCode,
                internalParkingId = event.parkingId,
                externalParkingId = externalParkingId,
                licensePlate = licensePlate,
                startTime = startTime,
                status = status
            )
        )
    }
}
