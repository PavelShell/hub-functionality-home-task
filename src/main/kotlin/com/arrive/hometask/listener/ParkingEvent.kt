package com.arrive.hometask.listener

import java.time.Instant

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
