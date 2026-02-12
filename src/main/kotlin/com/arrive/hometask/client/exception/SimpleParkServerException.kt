package com.arrive.hometask.client.exception

/**
 * Exception thrown when the external SimplePark API returns a 5xx server error,
 * is unreachable, or an unexpected error occurs.
 */
class SimpleParkServerException(message: String, cause: Throwable? = null) : SimpleParkException(message, cause)
