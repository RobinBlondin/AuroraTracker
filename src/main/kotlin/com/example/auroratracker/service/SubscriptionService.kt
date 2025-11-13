package com.example.auroratracker.service

import com.example.auroratracker.dto.SubscriptionDto
import com.example.auroratracker.dto.SubscriptionLiteDto
import com.example.auroratracker.dto.WeatherResponseDto
import com.example.auroratracker.mapper.SubscriptionMapper
import com.example.auroratracker.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*

@Service
class SubscriptionService(
      private val subRepo: SubscriptionRepository,
      private val jsonService: JsonService,
      private val mapper: SubscriptionMapper,
) {
      fun getAllSubs() = subRepo.findAll().map { mapper.toDto(it) }

      fun getSubByUserId(id: String): Optional<SubscriptionDto> {
            return subRepo.getByUserId(id).map { mapper.toDto(it) }
      }
      @Transactional
      fun saveSub(subDto: SubscriptionDto): SubscriptionDto = mapper.toEntity(subDto).let { mapper.toDto(subRepo.save(it)) }

    @Transactional
      fun deleteSubByUserId(id: String): Boolean {
            if (!subRepo.existsByUserId(id)) {
                  return false
            }
            subRepo.deleteByUserId(id)
            return true
      }

      @Transactional
      fun updateLonAndLat(dto: SubscriptionDto) {
            val sub = getSubByUserId(dto.userId!!).orElse(null) ?: return

            sub.lon = dto.lon
            sub.lat = dto.lat
            sub.updatedAt = LocalDateTime.now()

            saveSub(sub)
      }

      fun checkIfSubExists(dto: SubscriptionDto): Boolean = subRepo.existsByUserId(dto.userId!!)

      fun getWeatherData (dto: SubscriptionDto): WeatherResponseDto? {
            val url =
                  "https://api.open-meteo.com/v1/forecast?latitude=${dto.lat}&longitude=${dto.lon}&current=cloud_cover&daily=sunset"

            return jsonService.fetchAndParse<WeatherResponseDto>(url).getOrElse {
                  println("Failed to fetch or parse weather data: ${it.message}")
                  return null
            }
      }

      fun isAfterSunset(weatherData: WeatherResponseDto): Boolean {
            val currentTime = ZonedDateTime.parse("${weatherData.current?.time ?: return false}Z")
            val sunsetTime = ZonedDateTime.parse("${weatherData.daily?.sunset?.firstOrNull() ?: return false}Z")
            return currentTime.isAfter(sunsetTime)
      }

      fun getCloudCover(weatherData: WeatherResponseDto): Double {
            return weatherData.current?.cloudCover ?: 100.0
      }

      fun toLiteDto(dto: SubscriptionDto): SubscriptionLiteDto = SubscriptionLiteDto(dto.lon, dto.lat, dto.lastNotificationTime)

      fun updateLastNotificationTime(dto: SubscriptionDto): SubscriptionDto {
            val updatedUser = mapper.toEntity(dto).apply {
                  lastNotificationTime = ZonedDateTime.now(ZoneOffset.UTC)
            }
            return mapper.toDto(subRepo.save(updatedUser))
      }


      fun hasReceivedNotificationRecently(dto: SubscriptionDto): Boolean {
            return dto.lastNotificationTime?.let {
                  ZonedDateTime.now(ZoneOffset.UTC).minusHours(12).isBefore(it)
            } ?: false
      }
}