package com.example.auroratracker.controller

import com.example.auroratracker.config.AuroraPointsProperties
import com.example.auroratracker.dto.AuroraPointDto
import com.example.auroratracker.service.AuroraPointService
import com.example.auroratracker.service.AuroraPointsRateLimiter
import com.example.auroratracker.service.AuroraPointsResponseCache
import com.example.auroratracker.support.MutableClock
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Duration
import kotlin.test.assertNotNull

class AuroraPointsControllerTest {
    private val service = mockk<AuroraPointService>()
    private val clock = MutableClock()
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        val properties = AuroraPointsProperties(requestsPerMinute = 3)
        every { service.getAuroraPoints() } returns listOf(AuroraPointDto(18.0, 60.0, 50.0))
        mvc = MockMvcBuilders.standaloneSetup(AuroraPointsController(
            AuroraPointsResponseCache(service, jacksonObjectMapper(), properties, clock),
            AuroraPointsRateLimiter(properties, clock), clock
        )).build()
    }

    @Test
    fun `public map data is cached and includes browser caching headers`() {
        mvc.perform(get("/api/points/all"))
            .andExpect(status().isOk).andExpect(content().contentType("application/json"))
            .andExpect(jsonPath("$[0].lat").value(60.0))
            .andExpect(header().string("Cache-Control", "max-age=60, must-revalidate, public"))
            .andExpect(header().exists("ETag"))
        clock.advance(Duration.ofSeconds(20))
        mvc.perform(get("/api/points/all"))
            .andExpect(status().isOk)
            .andExpect(header().string("Cache-Control", "max-age=40, must-revalidate, public"))
        verify(exactly = 1) { service.getAuroraPoints() }
    }

    @Test
    fun `matching etags return not modified without another database read`() {
        val etag = mvc.perform(get("/api/points/all")).andReturn().response.getHeader("ETag")
        assertNotNull(etag)
        mvc.perform(get("/api/points/all").header("If-None-Match", etag))
            .andExpect(status().isNotModified).andExpect(content().string(""))
            .andExpect(header().string("ETag", etag))
        verify(exactly = 1) { service.getAuroraPoints() }
    }

    @Test
    fun `rate limits include retry after and cannot be bypassed by forwarded headers`() {
        repeat(3) { mvc.perform(get("/api/points/all")).andExpect(status().isOk) }
        mvc.perform(get("/api/points/all").header("X-Forwarded-For", "other-client"))
            .andExpect(status().isTooManyRequests)
            .andExpect(header().string("Retry-After", "60"))
            .andExpect(header().string("Cache-Control", "no-store"))
        verify(exactly = 1) { service.getAuroraPoints() }
        mvc.perform(get("/api/points/all").with { it.remoteAddr = "192.0.2.2"; it })
            .andExpect(status().isOk)
        clock.advance(Duration.ofSeconds(60))
        mvc.perform(get("/api/points/all")).andExpect(status().isOk)
    }

    @Test
    fun `conditional requests still count toward the rate limit`() {
        val etag = mvc.perform(get("/api/points/all")).andReturn().response.getHeader("ETag")!!
        repeat(2) {
            mvc.perform(get("/api/points/all").header("If-None-Match", etag)).andExpect(status().isNotModified)
        }
        mvc.perform(get("/api/points/all").header("If-None-Match", etag)).andExpect(status().isTooManyRequests)
    }

    @Test
    fun `expired snapshots return updated data rather than stale not modified`() {
        val etag = mvc.perform(get("/api/points/all")).andReturn().response.getHeader("ETag")!!
        every { service.getAuroraPoints() } returns listOf(AuroraPointDto(18.0, 60.0, 80.0))
        clock.advance(Duration.ofSeconds(60))
        mvc.perform(get("/api/points/all").header("If-None-Match", etag))
            .andExpect(status().isOk).andExpect(jsonPath("$[0].probability").value(80.0))
        verify(exactly = 2) { service.getAuroraPoints() }
    }
}
