package com.example.auroratracker.service

import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException

object SubscriptionValidation {
    fun email(value: String): String {
        val normalized = value.trim().lowercase(java.util.Locale.ROOT)
        if (normalized.length > 254 || !normalized.matches(Regex("""[^\s@]+@[^\s@]+\.[^\s@]+"""))) {
            invalid("Provide a valid email address")
        }
        return normalized
    }

    fun location(lat: Double?, lon: Double?) {
        if (lat?.let { !it.isFinite() || it !in -90.0..90.0 } == true ||
            lon?.let { !it.isFinite() || it !in -180.0..180.0 } == true) {
            invalid("Invalid coordinates")
        }
    }

    fun invalid(message: String): Nothing = throw ResponseStatusException(HttpStatus.BAD_REQUEST, message)
}
