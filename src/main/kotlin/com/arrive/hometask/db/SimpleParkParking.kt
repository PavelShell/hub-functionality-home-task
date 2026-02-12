package com.arrive.hometask.db

import com.arrive.hometask.client.model.SimpleParkParkingStatus
import jakarta.persistence.*
import java.time.Instant

/**
 * Entity representing a parking session in the local database.
 */
@Entity
class SimpleParkParking(

    /**
     * Primary key.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    var id: Long? = null,

    /**
     * Area code where the parking is located.
     */
    @Column(name = "area_code", nullable = false, length = 32)
    var areaCode: String = "",

    /**
     * Vehicle license plate.
     */
    @Column(name = "license_plate", nullable = false, length = 32)
    var licensePlate: String = "",

    /**
     * Current status of the parking (e.g., ACTIVE, STOPPED, FAILED).
     */
    @Column(name = "status", nullable = false, length = 8)
    @Enumerated(EnumType.STRING)
    var status: SimpleParkParkingStatus = SimpleParkParkingStatus.ACTIVE,

    /**
     * The time when the parking started.
     */
    @Column(name = "start_time", nullable = false)
    var startTime: Instant = Instant.now(),

    /**
     * The unique internal ID of the parking session.
     */
    @Column(name = "internal_parking_id", nullable = false)
    var internalParkingId: String = "",

    /**
     * Timestamp of the last update.
     */
    @Column(name = "updated_at")
    var updatedAt: Instant? = null,

    /**
     * Timestamp of when the record was created.
     */
    @Column(name = "created_at", updatable = false)
    var createdAt: Instant? = null,

    /**
     * Intended or actual end time of the parking session.
     */
    @Column(name = "end_time")
    var endTime: Instant? = null,

    /**
     * The ID of the parking session in the external system.
     */
    @Column(name = "external_parking_id")
    var externalParkingId: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SimpleParkParking) return false

        return id != null && id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }

    override fun toString(): String {
        return "SimpleParkParking(id=$id, internalParkingId='$internalParkingId', licensePlate='$licensePlate', status=$status)"
    }
}
