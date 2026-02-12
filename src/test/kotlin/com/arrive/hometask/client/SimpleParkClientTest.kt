package com.arrive.hometask.client

import com.arrive.hometask.client.exception.SimpleParkClientException
import com.arrive.hometask.client.exception.SimpleParkServerException
import com.arrive.hometask.client.model.SimpleParkParkingStatus
import com.github.tomakehurst.wiremock.WireMockServer
import com.github.tomakehurst.wiremock.client.WireMock.*
import com.github.tomakehurst.wiremock.core.WireMockConfiguration
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.web.client.RestTemplateBuilder
import java.time.Duration
import java.time.Instant

class SimpleParkClientTest {

    private lateinit var wireMock: WireMockServer

    private lateinit var client: SimpleParkClient

    private val apiKey = "test-api-key"

    @BeforeEach
    fun setUp() {
        wireMock = WireMockServer(WireMockConfiguration.options().dynamicPort())
        wireMock.start()
        configureFor("localhost", wireMock.port())
        client = SimpleParkClient(
            RestTemplateBuilder()
                .rootUri(wireMock.baseUrl())
                .defaultHeader("X-API-Key", apiKey)
                .setReadTimeout(Duration.ofSeconds(2))
                .setConnectTimeout(Duration.ofSeconds(2))
                .build()
        )
    }

    @AfterEach
    fun tearDown() {
        wireMock.stop()
    }

    @Test
    fun `should start parking successfully`() {
        val body = """{"parkingId":"EXT-1","status":"ACTIVE"}"""
        stubFor(
            post(urlEqualTo("/parking/start"))
                .withHeader("X-API-Key", equalTo(apiKey))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body))
        )

        val response = client.startParking("ABC-123", "AREA-1", Instant.now(), Instant.now().plusSeconds(3600))

        verify(postRequestedFor(urlEqualTo("/parking/start")).withHeader("X-API-Key", equalTo(apiKey)))
        assert(response.parkingId == "EXT-1")
        assert(response.status == SimpleParkParkingStatus.ACTIVE)
    }

    @Test
    fun `should extend parking successfully`() {
        val body = """{"parkingId":"EXT-1","status":"ACTIVE"}"""
        stubFor(
            post(urlEqualTo("/parking/EXT-1/extend"))
                .withHeader("X-API-Key", equalTo(apiKey))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body))
        )

        val response = client.extendParking("EXT-1", Instant.now().plusSeconds(7200))

        verify(postRequestedFor(urlEqualTo("/parking/EXT-1/extend")).withHeader("X-API-Key", equalTo(apiKey)))
        assert(response.parkingId == "EXT-1")
        assert(response.status == SimpleParkParkingStatus.ACTIVE)
    }

    @Test
    fun `should stop parking successfully`() {
        val body = """{"parkingId":"EXT-1","status":"STOPPED"}"""
        stubFor(
            post(urlEqualTo("/parking/EXT-1/stop"))
                .withHeader("X-API-Key", equalTo(apiKey))
                .willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body))
        )

        val response = client.stopParking("EXT-1", Instant.now())

        verify(postRequestedFor(urlEqualTo("/parking/EXT-1/stop")).withHeader("X-API-Key", equalTo(apiKey)))
        assert(response.parkingId == "EXT-1")
        assert(response.status == SimpleParkParkingStatus.STOPPED)
    }

    @Test
    fun `should throw client exception on 4xx`() {
        stubFor(
            post(urlEqualTo("/parking/start"))
                .withHeader("X-API-Key", equalTo(apiKey))
                .willReturn(aResponse().withStatus(400).withBody("bad request"))
        )

        assertThrows<SimpleParkClientException> {
            client.startParking("ABC-123", "AREA-1", Instant.now(), Instant.now().plusSeconds(3600))
        }
    }

    @Test
    fun `should throw server exception on 5xx`() {
        stubFor(
            post(urlEqualTo("/parking/start"))
                .withHeader("X-API-Key", equalTo(apiKey))
                .willReturn(aResponse().withStatus(500).withBody("server error"))
        )

        assertThrows<SimpleParkServerException> {
            client.startParking("ABC-123", "AREA-1", Instant.now(), Instant.now().plusSeconds(3600))
        }
    }

    @Test
    fun `should throw server exception on timeout`() {
        stubFor(
            post(urlEqualTo("/parking/start"))
                .withHeader("X-API-Key", equalTo(apiKey))
                .willReturn(
                    aResponse()
                        .withStatus(200)
                        .withFixedDelay(4000)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"parkingId\":\"EXT-1\",\"status\":\"ACTIVE\"}")
                )
        )

        assertThrows<SimpleParkServerException> {
            client.startParking("ABC-123", "AREA-1", Instant.now(), Instant.now().plusSeconds(3600))
        }
    }
}
