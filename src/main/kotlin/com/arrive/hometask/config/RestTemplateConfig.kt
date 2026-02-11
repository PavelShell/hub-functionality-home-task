package com.arrive.hometask.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.web.reactive.function.client.WebClientAutoConfiguration
import org.springframework.boot.web.client.RestTemplateBuilder
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestTemplate
import java.time.Duration

@Configuration
class RestTemplateConfig {

    @Bean
    fun simpleParkRestTemplate(
        @Value("\${simple-park.api.base-url}") baseUrl: String,
        @Value("\${simple-park.api.api-key}") apiKey: String,
        @Value("\${simple-park.api.timeout-seconds:5}") timeoutSeconds: Long,
    ): RestTemplate = RestTemplateBuilder()
        .rootUri(baseUrl)
        .defaultHeader("X-API-Key", apiKey)
        .setReadTimeout(Duration.ofSeconds(timeoutSeconds))
        .build()

}
