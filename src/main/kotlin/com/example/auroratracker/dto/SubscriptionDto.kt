package com.example.auroratracker.dto

import java.time.LocalDateTime
import java.time.ZonedDateTime

class SubscriptionDto(
      var id: Long? = null,
      var endpoint: String? = null,
      var p256dh: String? = null,
      var auth: String? = null,
      var userId: String? = null,
      var lon: Double? = null,
      var lat: Double? = null,
      var firebaseToken: String? = null,
      var lastNotificationTime: ZonedDateTime? = null,
      var updatedAt: LocalDateTime? = null,
)