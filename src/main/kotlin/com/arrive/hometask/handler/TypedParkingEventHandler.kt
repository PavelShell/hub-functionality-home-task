package com.arrive.hometask.handler

import com.arrive.hometask.listener.model.ParkingEventType

interface TypedParkingEventHandler : ParkingEventHandler {
    val eventType: ParkingEventType
}
