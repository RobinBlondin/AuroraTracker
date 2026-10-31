package com.example.auroratracker.entity

import jakarta.persistence.*
import java.time.ZonedDateTime

@Entity
data class Subscription(
      var email: String?,
      var endpoint: String? = null,
      @Column(columnDefinition = "TEXT")
      var p256dh: String? = null,
      @Column(columnDefinition = "TEXT")
      var auth: String? = null,
      var userId: String,
      var lon: Double? = null,
      var lat: Double? = null,
      var firebaseToken: String? = null,
      var lastNotificationTime: ZonedDateTime? = null,
      var enabled: Boolean = false,
      @Enumerated(EnumType.STRING)
      var notificationLevel: com.example.auroratracker.enums.NotificationLevel = com.example.auroratracker.enums.NotificationLevel.ALL,
): BaseEntity()