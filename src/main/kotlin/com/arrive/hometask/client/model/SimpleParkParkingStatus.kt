package com.arrive.hometask.client.model

/**
 * Possible statuses for a parking session in the SimplePark system.
 */
enum class SimpleParkParkingStatus {
    /**
     * Session is active and ongoing.
     */
    ACTIVE,
    /**
     * Session has been successfully stopped.
     */
    STOPPED,
    /**
     * Session failed to start, extend, or stop correctly.
     */
    FAILED
}
