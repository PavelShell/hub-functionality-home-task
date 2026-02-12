package com.arrive.hometask.handler

import com.arrive.hometask.listener.model.ParkingEventType
import org.springframework.stereotype.Component

/**
 * Factory for retrieving appropriate [ParkingEventHandler] based on [ParkingEventType].
 * Ensures that all event types have a corresponding handler registered during initialization.
 */
@Component
class ParkingEventHandlerFactory(
    parkingEventHandlers: Collection<TypedParkingEventHandler>
) {

    private val handlers = parkingEventHandlers.associateBy { it.eventType }

    init {
        // fail fast
        ParkingEventType.entries.forEach { requireNotNull(handlers[it]) { "No handler for event type $it" } }
    }

    /**
     * Retrieves the handler for the specified [eventType].
     *
     * @param eventType The type of parking event.
     * @return The corresponding [ParkingEventHandler].
     */
    fun getHandler(eventType: ParkingEventType): ParkingEventHandler = handlers[eventType]
    // should never happen
        ?: error("No handler for event type $eventType")
}
