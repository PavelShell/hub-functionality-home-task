package com.arrive.hometask.client.exception

/**
 * Exception thrown when the external SimplePark API returns a 4xx client error.
 */
class SimpleParkClientException(message: String, cause: Throwable? = null) : SimpleParkException(message, cause)
