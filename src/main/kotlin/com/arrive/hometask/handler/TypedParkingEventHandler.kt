package com.arrive.hometask.handler

import com.arrive.hometask.listener.ParkingEventType

interface TypedParkingEventHandler : ParkingEventHandler {
    val eventType: ParkingEventType
}
