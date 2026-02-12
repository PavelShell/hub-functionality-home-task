package com.arrive.hometask.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

/**
 * Configuration class for [RestTemplate] used to communicate with the SimplePark API.
 */
@Configuration
class RestTemplateConfig {

    /**
     * Creates and configures a [RestTemplate] bean with base URL, API key, and timeouts.
     *
     * @param baseUrl Base URL of the SimplePark API.
     * @param apiKey API key for authentication.
     * @param timeoutSeconds Connect and read timeout in seconds.
     * @return Configured [RestTemplate] instance.
     */
    @Bean
    fun simpleParkRestTemplate(
        @Value("\${simple-park.api.base-url}") baseUrl: String,
        @Value("\${simple-park.api.api-key}") apiKey: String,
        @Value("\${simple-park.api.timeout-seconds:5}") timeoutSeconds: Long,
    ): RestTemplate = RestTemplateBuilder()
        .rootUri(baseUrl)
        .defaultHeader("X-API-Key", apiKey)
        .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
        .setConnectTimeout(Duration.ofSeconds(timeoutSeconds))
        .build()

}
