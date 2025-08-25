package com.example.auroratracker.entity

import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime
import java.time.ZonedDateTime

@Entity
@Table(name = "users")
class User (
      var name: String? = null,
      var email: String? = null,
      var phoneNumber: String? = null,
      var lon: Double? = null,
      var lat: Double? = null,
      var lastNotificationTime: ZonedDateTime? = null,
): BaseEntity()