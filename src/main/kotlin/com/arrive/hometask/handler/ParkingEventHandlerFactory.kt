package com.arrive.hometask.handler

import com.arrive.hometask.listener.ParkingEventType
import org.springframework.stereotype.Component

@Component
class ParkingEventHandlerFactory(
    parkingEventHandlers: Collection<TypedParkingEventHandler>
) {

    private val handlers = parkingEventHandlers.associateBy { it.eventType }

    init {
        // fail fast
//        ParkingEventType.entries.forEach { requireNotNull(handlers[it]) { "No handler for event type $it" } }
    }

    fun getHandler(eventType: ParkingEventType): ParkingEventHandler = handlers[eventType]
    // should never happen
        ?: error("No handler for event type $eventType")
}
