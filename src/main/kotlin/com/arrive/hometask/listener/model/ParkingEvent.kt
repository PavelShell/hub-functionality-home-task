package com.arrive.hometask.listener.model

import java.time.Instant

/**
 * Represents a parking event received from an external source (e.g., Kafka).
 */
data class ParkingEvent(
    val eventType: ParkingEventType,
    val parkingId: String,
    val licensePlate: String? = null,
    val areaCode: String? = null,
    val startTime: Instant? = null,
    val endTime: Instant? = null,
    val priceAmount: Double? = null,
    val currency: String? = null
)
