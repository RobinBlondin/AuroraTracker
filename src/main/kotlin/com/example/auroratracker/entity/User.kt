package com.example.auroratracker.entity

import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Entity
@Table(name = "users")
class User (
      val name: String? = null,
      val email: String? = null,
      val phoneNumber: String? = null,
      val lon: Double? = null,
      val lat: Double? = null,
      var lastNotificationTime: ZonedDateTime? = null,
): BaseEntity()