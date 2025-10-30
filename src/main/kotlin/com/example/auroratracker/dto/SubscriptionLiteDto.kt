package com.example.auroratracker.dto

import java.time.ZonedDateTime

class SubscriptionLiteDto(
      var lon: Double? = null,
      var lat: Double? = null,
      var lastNotificationTime: ZonedDateTime? = null,
)