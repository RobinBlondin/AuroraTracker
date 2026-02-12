package com.example.auroratracker.entity

import com.example.auroratracker.enums.NotificationLevel
import jakarta.persistence.Entity
import java.time.ZonedDateTime

@Entity
class User(
      var email: String? = null,
      var phoneNumber: String? = null,
      var userId: String? = null,
      var lon: Double? = null,
      var lat: Double? = null,
      var lastNotificationTime: ZonedDateTime? = null,
      var notificationLevel: NotificationLevel? = null,
      var emailVerified: Boolean = false,
      var phoneVerified: Boolean = false,
      var enabled: Boolean = false,

      ): BaseEntity()