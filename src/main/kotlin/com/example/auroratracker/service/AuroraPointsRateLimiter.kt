package com.example.auroratracker.service

import com.example.auroratracker.config.AuroraPointsProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Component
class AuroraPointsRateLimiter(
    private val properties: AuroraPointsProperties,
    @param:Qualifier("auroraPointsClock") private val clock: Clock
) {
    private data class Window(val expiresAt: Instant, var count: Int = 0)
    private val clients = mutableMapOf<String, Window>()
    private var nextCleanup = Instant.MIN

    @Synchronized
    fun acquire(address: String): Long? {
        val now = clock.instant()
        if (!now.isBefore(nextCleanup)) {
            clients.entries.removeIf { !now.isBefore(it.value.expiresAt) }
            nextCleanup = now.plusSeconds(60)
        }
        var window = clients[address]
        if (window == null || !now.isBefore(window.expiresAt)) {
            if (window == null && clients.size >= properties.maxTrackedClients) return secondsUntil(now, nextCleanup)
            window = Window(now.plusSeconds(60))
            clients[address] = window
        }
        if (window.count >= properties.requestsPerMinute) return secondsUntil(now, window.expiresAt)
        window.count++
        return null
    }

    private fun secondsUntil(now: Instant, end: Instant): Long {
        val remaining = Duration.between(now, end)
        return (remaining.seconds + if (remaining.nano > 0) 1 else 0).coerceAtLeast(1)
    }
}
