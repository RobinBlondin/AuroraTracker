package com.example.auroratracker.dto

import java.time.ZonedDateTime

class UserDto(
      val id: Long? = null,
      val name: String? = null,
      val email: String? = null,
      val phoneNumber: String? = null,
      val lon: Double,
      val lat: Double,
      var lastNotificationTime: ZonedDateTime? = null
)