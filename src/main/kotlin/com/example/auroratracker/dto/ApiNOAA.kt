package com.example.auroratracker.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
class AuroraPointsDto {
      var coordinates: List<List<Int>>? = null
}

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