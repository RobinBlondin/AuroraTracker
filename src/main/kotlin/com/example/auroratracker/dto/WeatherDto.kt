package com.example.auroratracker.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class WeatherResponseDto {
      val current:  Current? = null
      val daily: Daily? = null

      override fun toString(): String {
            return "WeatherResponseDto(current=$current, daily=$daily)"
      }
}

@Serializable
class Current {
      val time: String? = null
      @SerialName("cloud_cover")
      val cloudCover: Double? = null

      override fun toString(): String {
            return "Current(time=$time, cloudCover=$cloudCover)"
      }
}

@Serializable
class Daily {
      val sunset: List<String>? = null
      override fun toString(): String {
            return "Daily(sunset=$sunset)"
      }
}