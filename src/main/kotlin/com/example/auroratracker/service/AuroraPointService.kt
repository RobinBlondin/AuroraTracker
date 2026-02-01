package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.dto.AuroraBelt
import com.example.auroratracker.dto.AuroraPointDto
import com.example.auroratracker.mapper.AuroraPointMapper
import com.example.auroratracker.repository.AuroraPointRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AuroraPointService(
      private val auroraPointRepository: AuroraPointRepository,
      private val auroraPointMapper: AuroraPointMapper
      private val jsonService: JsonService,
      private val env: EnvConfig

) {
      fun fetchAuroraPoints(): List<AuroraPointDto> {
            val url = env.api.url.noaa
            val response = jsonService.fetchAndParse<AuroraBelt>(url)
            val auroraBelt = response.getOrElse {
                  println("Failed to fetch or parse aurora points: ${it.message}")
                  AuroraBelt()
            }
            return auroraBelt.convertToAuroraPoints().filter { it.lat >= 30 }
      }

      @Scheduled(cron = "* * * * * *")
      fun updateAuroraPoints() {
            val points = fetchAuroraPoints()
            auroraPointRepository.deleteAll()
            points.forEach { auroraPointRepository.save(auroraPointMapper.toEntity(it)) }
      }
}