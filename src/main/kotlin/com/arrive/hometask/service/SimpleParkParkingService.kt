package com.arrive.hometask.service

import com.arrive.hometask.db.SimpleParkParking
import com.arrive.hometask.db.SimpleParkParkingJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class SimpleParkParkingService(
    private val parkParkingJpaRepository: SimpleParkParkingJpaRepository
) {

    @Transactional
    fun save(parking: SimpleParkParking) = parkParkingJpaRepository.save(parking)

    fun findByExternalId(externalId: String) = parkParkingJpaRepository.findOneByInternalParkingId(externalId)
}
