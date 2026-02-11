package com.arrive.hometask.client

import com.arrive.hometask.client.exception.SimpleParkClientException
import com.arrive.hometask.client.exception.SimpleParkServerException
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.HttpClientErrorException
import org.springframework.web.client.HttpServerErrorException
import org.springframework.web.client.ResourceAccessException
import org.springframework.web.client.RestTemplate
import java.time.Instant

/**
 * TODO: Implement REST client for SimplePark API
 *
 * Requirements:
 * - Call SimplePark API endpoints (start, extend, stop)
 * - Add X-API-Key header to all requests
 * - Configure timeout (5 seconds from properties)
 * - Handle errors appropriately (distinguish 4xx vs 5xx)
 *
 * Endpoints:
 * - POST /parking/start
 * - POST /parking/{parkingId}/extend
 * - POST /parking/{parkingId}/stop
 *
 * Hint: Use RestTemplate or WebClient
 */
@Component
class SimpleParkClient(
    private val restTemplate: RestTemplate
) {

    private val logger = LoggerFactory.getLogger(SimpleParkClient::class.java)

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

    fun extendParking(parkingId: String, newEndTime: Instant): SimpleParkClientResponse = postHandlingException(
        "/parking/$parkingId/extend",
        mapOf("newEndTime" to newEndTime)
    )

    fun stopParking(parkingId: String, actualEndTime: Instant): SimpleParkClientResponse = postHandlingException(
        "/parking/$parkingId/stop",
        mapOf("actualEndTime" to actualEndTime)
    )

    private fun postHandlingException(url: String, request: Any?): SimpleParkClientResponse {
        try {
            return requireNotNull(
                restTemplate.postForObject(url, request, SimpleParkClientResponse::class.java)
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
