package com.arrive.hometask.db

import org.springframework.data.jpa.repository.JpaRepository

interface SimpleParkParkingJpaRepository : JpaRepository<SimpleParkParking, Long> {

    fun findOneByInternalParkingId(internalParkingId: String): SimpleParkParking?
}
