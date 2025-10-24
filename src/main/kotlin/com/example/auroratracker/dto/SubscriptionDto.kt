package com.example.auroratracker.dto

import java.time.ZonedDateTime

class SubscriptionDto(
      var id: Long? = null,
      var endPoint: String? = null,
      var p256dh: String? = null,
      var auth: String? = null,
      var userId: String? = null,
      var lon: Double? = null,
      var lat: Double? = null,
      var lastNotificationTime: ZonedDateTime? = null,
)