package com.arrive.hometask.service

import com.arrive.hometask.db.SimpleParkParking
import com.arrive.hometask.db.SimpleParkParkingJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * Service for managing parking entity persistence and retrieval.
 */
@Service
class SimpleParkParkingService(
    private val parkParkingJpaRepository: SimpleParkParkingJpaRepository
) {

    /**
     * Saves or updates a parking entity.
     *
     * @param parking The parking entity to save.
     * @return The saved entity.
     */
    @Transactional
    fun save(parking: SimpleParkParking): SimpleParkParking = parkParkingJpaRepository.save(parking)

    /**
     * Finds a parking entity by its internal ID.
     *
     * @param id The internal parking ID.
     * @return The found [SimpleParkParking] or null if not found.
     */
    fun findByInternalParkingId(id: String): SimpleParkParking? = parkParkingJpaRepository.findOneByInternalParkingId(id)
}
