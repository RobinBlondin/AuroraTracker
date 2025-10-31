package com.example.auroratracker.entity

import jakarta.persistence.Entity

@Entity
class Notification(
      var userId: String? = null,
      var lon: Double? = null,
      var lat: Double? = null,
):BaseEntity()