package com.arrive.hometask.client

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

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
class SimpleParkClient() {

    private val logger = LoggerFactory.getLogger(SimpleParkClient::class.java)

}
