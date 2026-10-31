package com.example.auroratracker.dto

import com.example.auroratracker.enums.NotificationLevel
import java.time.LocalDateTime
import java.time.ZonedDateTime

class SubscriptionDto(
      var id: Long? = null,
      var email: String? = null,
      var endpoint: String? = null,
      var p256dh: String? = null,
      var auth: String? = null,
      var userId: String? = null,
      var lon: Double? = null,
      var lat: Double? = null,
      var firebaseToken: String? = null,
      var lastNotificationTime: ZonedDateTime? = null,
      var updatedAt: LocalDateTime? = null,
      var notificationLevel: NotificationLevel? = NotificationLevel.ALL,
      var enabled: Boolean? = false
)