package com.arrive.hometask.client.model

/**
 * Response from the SimplePark external API.
 *
 * @property parkingId The external ID assigned by SimplePark.
 * @property status The current status of the parking session in the external system.
 */
data class SimpleParkClientResponse(val parkingId: String, val status: SimpleParkParkingStatus)
