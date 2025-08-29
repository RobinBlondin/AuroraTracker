package com.example.auroratracker.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WeatherResponseDto {
      val current:  Current? = null
      val daily: Daily? = null
}

@Serializable
class Current {
      val time: String? = null
      @SerialName("cloud_cover")
      val cloudCover: Double? = null
}

@Serializable
class Daily {
      val sunset: List<String>? = null
}