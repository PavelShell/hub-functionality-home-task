package com.arrive.hometask.handler

import com.arrive.hometask.client.SimpleParkClient
import com.arrive.hometask.client.SimpleParkParkingStatus
import com.arrive.hometask.db.SimpleParkParking
import com.arrive.hometask.db.SimpleParkParkingJpaRepository
import com.arrive.hometask.listener.ParkingEvent
import com.arrive.hometask.listener.ParkingEventType
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class StartParkingEventHandler(
    override val eventType: ParkingEventType = ParkingEventType.PARKING_STARTED,
    private val parkClient: SimpleParkClient,
    private val parkParkingJpaRepository: SimpleParkParkingJpaRepository,
) : TypedParkingEventHandler {

    @Transactional
    override fun invoke(event: ParkingEvent) {
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
