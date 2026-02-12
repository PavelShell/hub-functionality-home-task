package com.arrive.hometask.db

import org.springframework.data.jpa.repository.JpaRepository

/**
 * Repository interface for [SimpleParkParking] entity.
 */
interface SimpleParkParkingJpaRepository : JpaRepository<SimpleParkParking, Long> {

    /**
     * Finds a single parking entity by its internal ID.
     *
     * @param internalParkingId The internal parking ID.
     * @return The found [SimpleParkParking] or null.
     */
    fun findOneByInternalParkingId(internalParkingId: String): SimpleParkParking?
}
