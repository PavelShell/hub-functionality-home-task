package com.arrive.hometask.handler

import com.arrive.hometask.listener.model.ParkingEvent

typealias ParkingEventHandler = (event: ParkingEvent) -> Unit
