package com.example.auroratracker.dto

import java.time.LocalDateTime

class UserDto(
      val id: Long? = null,
      val name: String? = null,
      val email: String? = null,
      val phoneNumber: String? = null,
      val lon: Double,
      val lat: Double,
      var lastNotificationTime: LocalDateTime? = null
)