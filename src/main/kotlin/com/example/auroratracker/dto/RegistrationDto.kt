package com.example.auroratracker.dto

import com.example.auroratracker.enums.NotificationLevel

data class RegistrationDto(
    val email: String? = null,
    val endpoint: String? = null,
    val p256dh: String? = null,
    val auth: String? = null,
    val firebaseToken: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
    val notificationLevel: NotificationLevel
)
