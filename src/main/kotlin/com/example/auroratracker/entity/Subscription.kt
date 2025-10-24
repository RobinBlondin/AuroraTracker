package com.example.auroratracker.entity

import jakarta.persistence.*
import java.time.ZonedDateTime

@Entity
data class Subscription(
      var endPoint: String,
      @Column(columnDefinition = "TEXT")
      var p256dh: String,
      @Column(columnDefinition = "TEXT")
      var auth: String,
      var userId: String,
      var lon: Double? = null,
      var lat: Double? = null,
      var lastNotificationTime: ZonedDateTime? = null,
): BaseEntity()