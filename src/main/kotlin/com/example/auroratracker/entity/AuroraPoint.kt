package com.example.auroratracker.entity

import jakarta.persistence.Entity

@Entity
class AuroraPoint(
      var lat: Double? = null,
      var lon: Double? = null,
      var probability: Double? = null
): BaseEntity()