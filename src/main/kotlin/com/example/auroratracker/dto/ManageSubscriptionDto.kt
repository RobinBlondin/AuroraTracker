package com.example.auroratracker.dto

import com.example.auroratracker.enums.NotificationLevel

data class ManageSubscriptionDto(
    val lat: Double,
    val lon: Double,
    val notificationLevel: NotificationLevel,
    val enabled: Boolean
)

data class ManageSubscriptionPatchDto(
    val lat: Double? = null,
    val lon: Double? = null,
    val notificationLevel: NotificationLevel? = null,
    val enabled: Boolean? = null
)
