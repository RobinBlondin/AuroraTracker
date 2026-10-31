package com.example.auroratracker.service

import com.example.auroratracker.config.AuroraPointsProperties
import com.example.auroratracker.support.MutableClock
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.*

class AuroraPointsRateLimiterTest {
    private val clock = MutableClock()

    @Test
    fun `limits are per client and reset after one minute`() {
        val limiter = AuroraPointsRateLimiter(AuroraPointsProperties(requestsPerMinute = 2), clock)
        repeat(2) { assertNull(limiter.acquire("client-a")) }
        assertEquals(60L, limiter.acquire("client-a"))
        assertNull(limiter.acquire("client-b"))
        clock.advance(Duration.ofMillis(59_100))
        assertEquals(1L, limiter.acquire("client-a"))
        clock.advance(Duration.ofMillis(900))
        assertNull(limiter.acquire("client-a"))
    }

    @Test
    fun `client tracking is bounded and expired entries are reclaimed`() {
        val limiter = AuroraPointsRateLimiter(AuroraPointsProperties(maxTrackedClients = 1), clock)
        assertNull(limiter.acquire("client-a"))
        assertEquals(60L, limiter.acquire("client-b"))
        assertNull(limiter.acquire("client-a"))
        clock.advance(Duration.ofSeconds(60))
        assertNull(limiter.acquire("client-b"))
    }

    @Test
    fun `parallel requests cannot exceed allowance`() {
        val limiter = AuroraPointsRateLimiter(AuroraPointsProperties(requestsPerMinute = 3), clock)
        val executor = Executors.newFixedThreadPool(8)
        val start = CountDownLatch(1)
        try {
            val futures = (1..20).map { executor.submit(Callable { start.await(); limiter.acquire("client") }) }
            start.countDown()
            val results = futures.map { it.get(3, TimeUnit.SECONDS) }
            assertEquals(3, results.count { it == null })
        } finally {
            executor.shutdownNow()
        }
    }
}
