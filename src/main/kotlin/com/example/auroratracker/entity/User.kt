package com.example.auroratracker.entity

import java.time.LocalDateTime

class User(
      val name: String? = null,
      val email: String? = null,
      val phoneNumber: String? = null,
      val lon: Double? = null,
      val lat: Double? = null,
      val lastNotificationTime: LocalDateTime? = null,
): BaseEntity()