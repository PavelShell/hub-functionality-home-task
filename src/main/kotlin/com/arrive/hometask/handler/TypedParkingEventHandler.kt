package com.arrive.hometask.handler

import com.arrive.hometask.listener.model.ParkingEventType

/**
 * Interface for typed parking event handlers.
 * Associates a handler with a specific [ParkingEventType].
 */
interface TypedParkingEventHandler : ParkingEventHandler {
    /**
     * The type of event this handler is responsible for.
     */
    val eventType: ParkingEventType
}
