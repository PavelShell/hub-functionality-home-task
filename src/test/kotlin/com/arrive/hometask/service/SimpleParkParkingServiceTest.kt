package com.arrive.hometask.service

import com.arrive.hometask.client.model.SimpleParkParkingStatus
import com.arrive.hometask.config.DbAwareTestConfig
import com.arrive.hometask.db.SimpleParkParking
import com.arrive.hometask.db.SimpleParkParkingJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.time.Instant

@SpringBootTest
class SimpleParkParkingServiceTest : DbAwareTestConfig() {

    @Autowired(required = false)
    private lateinit var parkParkingJpaRepository: SimpleParkParkingJpaRepository

    @Autowired(required = false)
    private lateinit var service: SimpleParkParkingService

    @BeforeEach
    @AfterEach
    fun `clean up database`() {
        parkParkingJpaRepository.deleteAllInBatch()
    }

    @Test
    fun `should save parking data`() {
        val parkingEvent = SimpleParkParking(
            areaCode = "areaCode",
            internalParkingId = "internalParkingId",
            licensePlate = "licensePlate",
            startTime = Instant.now(),
            status = SimpleParkParkingStatus.ACTIVE
        )

        service.save(parkingEvent)

        val createdParking = parkParkingJpaRepository.findOneByInternalParkingId(parkingEvent.internalParkingId)
        assertNotNull(createdParking)
        assertNotNull(createdParking?.createdAt)
        assertNotNull(createdParking?.updatedAt)
    }

    @Test
    fun `should find parking by external id`() {
        val parkingEvent = SimpleParkParking(
            areaCode = "areaCode",
            internalParkingId = "internalParkingId",
            licensePlate = "licensePlate",
            startTime = Instant.now(),
            status = SimpleParkParkingStatus.ACTIVE
        )
        parkParkingJpaRepository.save(parkingEvent)

        val foundParking = service.findByInternalParkingId(parkingEvent.internalParkingId)

        assertNotNull(foundParking)
    }
}
