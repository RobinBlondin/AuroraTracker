package com.example.auroratracker.entity

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
      var refreshToken: String? = null,
      var accessToken: String? = null,
      var emailVerified: Boolean = false,
      var phoneVerified: Boolean = false,
      var enabled: Boolean = false,

): BaseEntity()