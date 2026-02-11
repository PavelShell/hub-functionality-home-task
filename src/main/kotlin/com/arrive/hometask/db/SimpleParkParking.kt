package com.arrive.hometask.db

import com.arrive.hometask.client.SimpleParkParkingStatus
import jakarta.persistence.*
import java.time.Instant

@Entity
data class SimpleParkParking(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null,

    @Column(name = "area_code", nullable = false, length = 32)
    var areaCode: String,

    @Column(name = "license_plate", nullable = false, length = 32)
    var licensePlate: String,

    @Column(name = "status", nullable = false, length = 8)
    @Enumerated(EnumType.STRING)
    var status: SimpleParkParkingStatus,

    @Column(name = "start_time", nullable = false)
    var startTime: Instant,

    @Column(name = "internal_parking_id", nullable = false)
    var internalParkingId: String,

    @Column(name = "updated_at")
    var updatedAt: Instant? = null,

    @Column(name = "created_at")
    var createdAt: Instant? = null,


    @Column(name = "end_time")
    var endTime: Instant? = null,

    @Column(name = "external_parking_id")
    var externalParkingId: String? = null,
)
