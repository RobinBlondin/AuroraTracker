package com.example.auroratracker.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class AuroraBelt(val coordinates: List<List<Double>> = emptyList()) {
      fun convertToAuroraPoints(): List<AuroraPoint> {
            return coordinates.map { cord ->
                  AuroraPoint(
                        lon = cord[0],
                        lat = cord[1],
                        probability = cord[2]
                  )
            }
      }
}

data class AuroraPoint(
      var lon: Double,
      var lat: Double,
      var probability: Double
)


@Serializable
class KpIndexDto {
      @SerialName("time_tag")
      val timeTag: String? = null
      @SerialName("kp_index")
      val kpIndex: Int? = null
      @SerialName("estimated_kp")
      val estimatedKpIndex: Double? = null
      val kp: String? = null
}