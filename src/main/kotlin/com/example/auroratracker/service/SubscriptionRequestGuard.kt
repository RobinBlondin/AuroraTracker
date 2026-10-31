package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.server.ResponseStatusException
import java.net.URI

@Component
class SubscriptionRequestGuard(
    private val limiter: SubscriptionRequestRateLimiter,
    private val env: EnvConfig
) {
    fun check(request: HttpServletRequest, purpose: SubscriptionRequestRateLimiter.Purpose = SubscriptionRequestRateLimiter.Purpose.RECOVERY) {
        if (request.getHeader(purpose.header) != "1" || request.getHeader("Sec-Fetch-Site") == "cross-site") {
            throw ResponseStatusException(HttpStatus.FORBIDDEN)
        }
        val origin = request.getHeader("Origin")
        if (origin != null) {

            val expected = URI(env.resend.verificationUrl)
            val actual = runCatching { URI(origin) }.getOrNull()
            if (actual == null || actual.scheme != expected.scheme || actual.authority != expected.authority) {
                throw ResponseStatusException(HttpStatus.FORBIDDEN)
            }
        }
        limiter.check(request.remoteAddr, purpose)
    }
}
