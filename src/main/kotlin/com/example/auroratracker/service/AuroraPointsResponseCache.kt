package com.example.auroratracker.service

import com.example.auroratracker.config.AuroraPointsProperties
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Component
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant

@Component
class AuroraPointsResponseCache(
    private val points: AuroraPointService,
    private val mapper: ObjectMapper,
    private val properties: AuroraPointsProperties,
    @param:Qualifier("auroraPointsClock") private val clock: Clock
) {
    data class Snapshot(val json: String, val etag: String, val expiresAt: Instant)
    private var cached: Snapshot? = null

    @Synchronized
    fun get(): Snapshot {
        cached?.takeIf { clock.instant().isBefore(it.expiresAt) }?.let { return it }
        val json = mapper.writeValueAsString(points.getAuroraPoints())
        val hash = MessageDigest.getInstance("SHA-256").digest(json.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
        return Snapshot(json, "\"$hash\"", clock.instant().plus(properties.cacheTtl)).also { cached = it }
    }
}
