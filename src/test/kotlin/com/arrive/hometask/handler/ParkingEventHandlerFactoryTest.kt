package com.arrive.hometask.handler

import com.arrive.hometask.listener.model.ParkingEventType
import io.mockk.every
import io.mockk.junit5.MockKExtension
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(MockKExtension::class)
class ParkingEventHandlerFactoryTest {

    @Test
    fun `should initialize successfully when all handlers are provided`() {
        // given
        val handlers = ParkingEventType.entries.map { type ->
            mockk<TypedParkingEventHandler>().apply {
                every { this@apply.eventType } returns type
            }
        }

        // when
        val factory = ParkingEventHandlerFactory(handlers)

        // then
        ParkingEventType.entries.forEach { type ->
            assertEquals(type, (factory.getHandler(type) as TypedParkingEventHandler).eventType)
        }
    }

    @Test
    fun `should throw exception during initialization when a handler is missing`() {
        // given
        val incompleteHandlers = listOf(
            mockk<TypedParkingEventHandler>().apply {
                every { this@apply.eventType } returns ParkingEventType.PARKING_STARTED
            }
        )

        // when & then
        assertThrows<IllegalArgumentException> {
            ParkingEventHandlerFactory(incompleteHandlers)
        }
    }

    @Test
    fun `should return correct handler for specific event type`() {
        // given
        val startedHandler = mockk<TypedParkingEventHandler>().apply {
            every { this@apply.eventType } returns ParkingEventType.PARKING_STARTED
        }
        val extendedHandler = mockk<TypedParkingEventHandler>().apply {
            every { this@apply.eventType } returns ParkingEventType.PARKING_EXTENDED
        }
        val stoppedHandler = mockk<TypedParkingEventHandler>().apply {
            every { this@apply.eventType } returns ParkingEventType.PARKING_STOPPED
        }
        val factory = ParkingEventHandlerFactory(listOf(startedHandler, extendedHandler, stoppedHandler))

        // when
        val result = factory.getHandler(ParkingEventType.PARKING_EXTENDED)

        // then
        assertEquals(extendedHandler, result)
    }
}
