package com.example.auroratracker.service

import com.example.auroratracker.config.AuroraPointsProperties
import com.example.auroratracker.dto.AuroraPointDto
import com.example.auroratracker.support.MutableClock
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.*
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

class AuroraPointsResponseCacheTest {
    private val service = mockk<AuroraPointService>()
    private val clock = MutableClock()
    private val cache = AuroraPointsResponseCache(service, jacksonObjectMapper(), AuroraPointsProperties(), clock)

    @Test
    fun `requests reuse the same serialized snapshot until expiry`() {
        every { service.getAuroraPoints() } returns listOf(AuroraPointDto(18.0, 60.0, 50.0))
        val first = cache.get()
        clock.advance(Duration.ofSeconds(59))
        assertSame(first, cache.get())
        assertTrue(first.json.contains("\"probability\":50.0"))
        verify(exactly = 1) { service.getAuroraPoints() }
    }

    @Test
    fun `expiry refreshes data and etag`() {
        every { service.getAuroraPoints() } returnsMany listOf(
            listOf(AuroraPointDto(18.0, 60.0, 50.0)), listOf(AuroraPointDto(18.0, 60.0, 80.0))
        )
        val first = cache.get()
        clock.advance(Duration.ofSeconds(60))
        val updated = cache.get()
        assertNotEquals(first.etag, updated.etag)
        assertTrue(updated.json.contains("\"probability\":80.0"))
        verify(exactly = 2) { service.getAuroraPoints() }
    }

    @Test
    fun `unchanged data keeps the same etag after refresh`() {
        every { service.getAuroraPoints() } returns emptyList()
        val first = cache.get()
        clock.advance(Duration.ofSeconds(60))
        assertEquals(first.etag, cache.get().etag)
        assertEquals("[]", first.json)
    }

    @Test
    fun `failed loads are not cached`() {
        every { service.getAuroraPoints() } throws IllegalStateException("database unavailable") andThen emptyList()
        assertFailsWith<IllegalStateException> { cache.get() }
        assertEquals("[]", cache.get().json)
        verify(exactly = 2) { service.getAuroraPoints() }
    }

    @Test
    fun `concurrent requests perform only one cache load`() {
        every { service.getAuroraPoints() } returns listOf(AuroraPointDto(18.0, 60.0, 50.0))
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        try {
            val futures = (1..8).map { executor.submit(Callable { start.await(); cache.get() }) }
            start.countDown()
            val snapshots = futures.map { it.get(3, TimeUnit.SECONDS) }
            snapshots.forEach { assertSame(snapshots.first(), it) }
            verify(exactly = 1) { service.getAuroraPoints() }
        } finally {
            executor.shutdownNow()
        }
    }
}
