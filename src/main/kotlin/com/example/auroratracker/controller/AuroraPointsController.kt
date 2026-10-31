package com.example.auroratracker.controller

import com.example.auroratracker.service.AuroraPointsRateLimiter
import com.example.auroratracker.service.AuroraPointsResponseCache
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.CacheControl
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.context.request.ServletWebRequest
import java.time.Clock
import java.time.Duration

@RestController
@RequestMapping("/api/points")
class AuroraPointsController(
    private val cache: AuroraPointsResponseCache,
    private val limiter: AuroraPointsRateLimiter,
    @param:Qualifier("auroraPointsClock") private val clock: Clock
) {
    @GetMapping("/all", produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getPoints(request: ServletWebRequest): ResponseEntity<String> {
        limiter.acquire(request.request.remoteAddr)?.let { retryAfter ->
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, retryAfter.toString())
                .cacheControl(CacheControl.noStore())
                .body("""{"message":"Too many map requests. Please try again later."}""")
        }
        val snapshot = cache.get()
        val remaining = Duration.between(clock.instant(), snapshot.expiresAt).seconds.coerceAtLeast(0)
        val cacheControl = CacheControl.maxAge(Duration.ofSeconds(remaining)).cachePublic().mustRevalidate()
        if (request.checkNotModified(snapshot.etag)) {
            return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
                .cacheControl(cacheControl).eTag(snapshot.etag).build()
        }
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON)
            .cacheControl(cacheControl).eTag(snapshot.etag).body(snapshot.json)
    }
}
