package com.arrive.hometask.listener

import com.arrive.hometask.handler.ParkingEventHandlerFactory
import com.arrive.hometask.listener.model.ParkingEvent
import org.slf4j.LoggerFactory
import org.springframework.kafka.annotation.KafkaListener
import org.springframework.kafka.support.Acknowledgment
import org.springframework.stereotype.Component

@Component
class ParkingEventListener(
    private val handlersFactory: ParkingEventHandlerFactory
) {

    private val logger = LoggerFactory.getLogger(ParkingEventListener::class.java)

    @KafkaListener(topics = ["parking.events"], errorHandler = "listenerErrorHandler")
    fun processMessage(event: ParkingEvent, ack: Acknowledgment) {
        logger.info("Received parking event: $event")
        handlersFactory.getHandler(event.eventType).invoke(event)
        ack.acknowledge()
    }
}
