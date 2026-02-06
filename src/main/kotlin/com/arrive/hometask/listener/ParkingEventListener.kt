package com.arrive.hometask.listener

import org.slf4j.LoggerFactory
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
class ParkingEventListener {

    private val logger = LoggerFactory.getLogger(ParkingEventListener::class.java)

    // TODO: Implement Kafka listener
}
