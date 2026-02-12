package com.arrive.hometask.listener.model

/**
 * Enum representing the different types of parking events.
 */
enum class ParkingEventType {
    /**
     * Triggered when a new parking session is started.
     */
    PARKING_STARTED,

    /**
     * Triggered when an existing parking session is extended.
     */
    PARKING_EXTENDED,

    /**
     * Triggered when a parking session is stopped.
     */
    PARKING_STOPPED
}
