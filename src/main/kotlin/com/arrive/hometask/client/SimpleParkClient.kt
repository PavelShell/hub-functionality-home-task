package com.arrive.hometask.client

import com.arrive.hometask.client.exception.SimpleParkClientException
import com.arrive.hometask.client.exception.SimpleParkServerException
import com.arrive.hometask.client.model.SimpleParkClientResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import org.springframework.web.client.postForObject
import java.time.Instant

/**
 * Client for interacting with the SimplePark external API.
 * Provides methods to start, extend, and stop parking sessions.
 */
@Component
class SimpleParkClient(
    private val restTemplate: RestTemplate
) {

    private val logger = LoggerFactory.getLogger(SimpleParkClient::class.java)

    /**
     * Sends a request to start a parking session.
     *
     * @param licensePlate Vehicle license plate.
     * @param areaCode Code of the parking area.
     * @param startTime Intended start time of the parking.
     * @param endTime Optional intended end time of the parking.
     * @return [SimpleParkClientResponse] containing the external parking ID and status.
     * @throws SimpleParkClientException if the client returns a 4xx error.
     * @throws SimpleParkServerException if the client returns a 5xx error or is unreachable.
     */
    fun startParking(
        licensePlate: String,
        areaCode: String,
        startTime: Instant,
        endTime: Instant?
    ): SimpleParkClientResponse = postHandlingException(
        "/parking/start",
        mapOf(
            "licensePlate" to licensePlate,
            "areaCode" to areaCode,
            "startTime" to startTime,
            "endTime" to endTime,
        )
    )

    /**
     * Sends a request to extend an existing parking session.
     *
     * @param parkingId The external parking ID.
     * @param newEndTime The new intended end time.
     * @return [SimpleParkClientResponse] with updated status.
     * @throws SimpleParkClientException if the client returns a 4xx error.
     * @throws SimpleParkServerException if the client returns a 5xx error or is unreachable.
     */
    fun extendParking(parkingId: String, newEndTime: Instant): SimpleParkClientResponse = postHandlingException(
        "/parking/$parkingId/extend",
        mapOf("newEndTime" to newEndTime)
    )

    /**
     * Sends a request to stop a parking session.
     *
     * @param parkingId The external parking ID.
     * @param actualEndTime The actual time when the parking stopped.
     * @return [SimpleParkClientResponse] with final status.
     * @throws SimpleParkClientException if the client returns a 4xx error.
     * @throws SimpleParkServerException if the client returns a 5xx error or is unreachable.
     */
    fun stopParking(parkingId: String, actualEndTime: Instant): SimpleParkClientResponse = postHandlingException(
        "/parking/$parkingId/stop",
        mapOf("actualEndTime" to actualEndTime)
    )

    private fun postHandlingException(url: String, request: Any?): SimpleParkClientResponse {
        try {
            return requireNotNull(
                restTemplate.postForObject<SimpleParkClientResponse>(url, request)
            )
        } catch (ex: HttpClientErrorException) {
            // 4xx - Bad Request, Unauthorized, Not Found
            logger.error("SimplePark client error (4xx): ${ex.statusCode} - ${ex.responseBodyAsString}", ex)
            throw SimpleParkClientException("Client error: ${ex.statusCode}", ex)
        } catch (ex: HttpServerErrorException) {
            // 5xx - Internal Server Error
            logger.warn("SimplePark server error (5xx): ${ex.statusCode}", ex)
            // Can be retried
            throw SimpleParkServerException("Server error: ${ex.statusCode}", ex)
        } catch (ex: ResourceAccessException) {
            // Timeouts, Connection Refused
            logger.warn("SimplePark unreachable: ${ex.message}", ex)
            throw SimpleParkServerException("Service unavailable", ex)
        } catch (ex: Exception) {
            logger.error("Unexpected error calling SimplePark", ex)
            throw SimpleParkServerException("Unexpected error", ex)
        }
    }
}
