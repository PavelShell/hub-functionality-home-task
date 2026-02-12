package com.arrive.hometask.handler

import com.arrive.hometask.listener.model.ParkingEvent

/**
 * Type alias for a function that handles a [ParkingEvent].
 */
typealias ParkingEventHandler = (event: ParkingEvent) -> Unit
