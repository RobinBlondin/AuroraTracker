package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.dto.AuroraBelt
import com.example.auroratracker.dto.AuroraPointDto
import com.example.auroratracker.mapper.AuroraPointMapper
import com.example.auroratracker.repository.AuroraPointRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service

@Service
class AuroraPointService(
      private val auroraPointRepository: AuroraPointRepository,
      private val auroraPointMapper: AuroraPointMapper,
      private val jsonService: JsonService,
      private val env: EnvConfig

) {
      private val log = LoggerFactory.getLogger(this::class.java)

      fun fetchAuroraPoints(): List<AuroraPointDto> {
            val url = env.api.url.noaa
            val response = jsonService.fetchAndParse<AuroraBelt>(url)
            val auroraBelt = response.getOrElse {
                  println("Failed to fetch or parse aurora points: ${it.message}")
                  AuroraBelt()
            }
            return auroraBelt.convertToAuroraPoints().filter { it.lat >= 30 }
      }

      @Scheduled(cron = "0 0,30 * * * *")
      fun updateAuroraPoints() {
            val points = fetchAuroraPoints()
            auroraPointRepository.deleteAll()
            points.filter { it.probability > 2 }.forEach { auroraPointRepository.save(auroraPointMapper.toEntity(it)) }
            log.info("Aurora points fetched and saved to database")
      }

      fun getAuroraPoints(): List<AuroraPointDto> {
            return auroraPointRepository.findAll().map { auroraPointMapper.toDto(it) }
      }

}