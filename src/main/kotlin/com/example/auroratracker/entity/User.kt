package com.example.auroratracker.entity

import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "users")
class User (
      val name: String? = null,
      val email: String? = null,
      val phoneNumber: String? = null,
      val lon: Double? = null,
      val lat: Double? = null,
      val lastNotificationTime: LocalDateTime? = null,
): BaseEntity()