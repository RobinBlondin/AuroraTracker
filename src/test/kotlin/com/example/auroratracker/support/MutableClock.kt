package com.example.auroratracker.support

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicReference

class MutableClock : Clock() {
    private val current = AtomicReference(Instant.parse("2026-10-30T12:00:00Z"))
    fun advance(duration: Duration) { current.updateAndGet { it.plus(duration) } }
    override fun instant(): Instant = current.get()
    override fun getZone(): ZoneId = ZoneOffset.UTC
    override fun withZone(zone: ZoneId): Clock = Clock.fixed(instant(), zone)
}
