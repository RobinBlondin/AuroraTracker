package com.example.auroratracker.service

import com.example.auroratracker.dto.SubscriptionDto
import org.springframework.web.server.ResponseStatusException
import com.example.auroratracker.dto.WeatherResponseDto
import com.example.auroratracker.entity.EmailVerification
import com.example.auroratracker.mapper.SubscriptionMapper
import com.example.auroratracker.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import com.example.auroratracker.dto.ManageSubscriptionDto
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.*
import com.example.auroratracker.dto.ManageSubscriptionPatchDto

@Service
class SubscriptionService(
      private val subRepo: SubscriptionRepository,
      private val jsonService: JsonService,
      private val subMapper: SubscriptionMapper,
    private val emailVerificationService: EmailVerificationService,
) {
      private fun managedEntity(userId: String) = subRepo.getByUserId(userId).orElseThrow {
            ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND)
      }

      private fun managedDto(sub: com.example.auroratracker.entity.Subscription) =
            ManageSubscriptionDto(
                  sub.lat ?: 0.0, sub.lon ?: 0.0, sub.notificationLevel, sub.enabled
            )

      @Transactional(readOnly = true)
      fun getManagedSubscription(userId: String) = managedDto(managedEntity(userId))

      @Transactional
      fun updateManagedSubscription(userId: String, patch: ManageSubscriptionPatchDto): ManageSubscriptionDto {
            SubscriptionValidation.location(patch.lat, patch.lon)
            val sub = managedEntity(userId)
            patch.lat?.let { sub.lat = it }
            patch.lon?.let { sub.lon = it }
            patch.notificationLevel?.let { sub.notificationLevel = it }
            patch.enabled?.let { sub.enabled = it }
            return managedDto(subRepo.save(sub))
      }

      @Transactional
      fun deleteManagedSubscription(userId: String) {
            val sub = managedEntity(userId)
            emailVerificationService.repo.deleteBySubscription_UserId(userId)
            subRepo.delete(sub)
      }

      fun getAllSubs() = subRepo.findAll().map { subMapper.toDto(it) }

      fun getSubByUserId(id: String): Optional<SubscriptionDto> {
            return subRepo.getByUserId(id).map { subMapper.toDto(it) }
      }
      @Transactional
      fun saveSub(subDto: SubscriptionDto): SubscriptionDto = subMapper.toEntity(subDto).let { subMapper.toDto(subRepo.save(it)) }

    @Transactional
      fun deleteSubByUserId(id: String): Boolean {
            if (!subRepo.existsByUserId(id)) {
                  return false
            }
            subRepo.deleteByUserId(id)
            return true
      }

    @Transactional
    fun createSubscription(dto: SubscriptionDto): EmailVerification {
        val subscription = saveSub(dto)


        val verification = EmailVerification(
            subscription = subMapper.toEntity(subscription),
            token = UUID.randomUUID().toString()
        )

        emailVerificationService.save(verification)
        return verification
    }

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

      fun updateLastNotificationTime(dto: SubscriptionDto): SubscriptionDto {
            val updatedUser = subMapper.toEntity(dto).apply {
                  lastNotificationTime = ZonedDateTime.now(ZoneOffset.UTC)
            }
            return subMapper.toDto(subRepo.save(updatedUser))
      }


      fun hasReceivedNotificationRecently(dto: SubscriptionDto): Boolean {
            return dto.lastNotificationTime?.let {
                  ZonedDateTime.now(ZoneOffset.UTC).minusHours(12).isBefore(it)
            } ?: false
      }

    @Transactional
    fun handleInvalidPushSubscription(
        sub: SubscriptionDto
    ): Boolean {
        if (sub.email.isNullOrBlank()) {
            deleteSubByUserId(sub.userId!!)
            return false
        } else {
            sub.auth = null
            sub.p256dh = null
            sub.endpoint = null
            sub.firebaseToken = null
            saveSub(sub)
            return true
        }
    }
}