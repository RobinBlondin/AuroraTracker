package com.example.auroratracker.service

import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.time.Instant

@Component
class SubscriptionRequestRateLimiter {
    enum class Purpose(val header: String, val limit: Int) {
        RECOVERY("X-Management-Request", 10),
        SIGNUP("X-Subscription-Request", 5)
    }
    private data class Key(val address: String, val purpose: Purpose)
    private data class Window(val start: Instant, var count: Int)
    private val windows = mutableMapOf<Key, Window>()

    @Synchronized
    fun check(address: String, purpose: Purpose = Purpose.RECOVERY) {
        val now = Instant.now()
        windows.entries.removeIf { !now.isBefore(it.value.start.plusSeconds(600)) }
        val key = Key(address, purpose)
        val window = windows[key] ?: run {
            if (windows.size >= 10000) reject()
            Window(now, 0).also { windows[key] = it }
        }
        if (++window.count > purpose.limit) reject()
    }

    private fun reject(): Nothing = throw ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Please try again later")
}
