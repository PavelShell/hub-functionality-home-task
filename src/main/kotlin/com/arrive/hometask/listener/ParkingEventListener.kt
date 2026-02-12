package com.arrive.hometask.listener

import com.arrive.hometask.handler.ParkingEventHandlerFactory
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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
    private val handlersFactory: ParkingEventHandlerFactory
) {

    private val logger = LoggerFactory.getLogger(ParkingEventListener::class.java)

    // Transactions?
    @KafkaListener(topics = ["parking.events"], errorHandler = "listenerErrorHandler")
    @Transactional
    fun processMessage(event: ParkingEvent, ack: Acknowledgment) {
        logger.info("Received event: $event")
        handlersFactory.getHandler(event.eventType).invoke(event)
        ack.acknowledge()
    }

    private fun handleParkingStopped(event: ParkingEvent) {
        TODO("Not yet implemented")
    }

    private fun handleParkingExtended(event: ParkingEvent) {
        TODO("Not yet implemented")
    }
}
