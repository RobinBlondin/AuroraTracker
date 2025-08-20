package com.example.auroratracker.service

import com.example.auroratracker.Thresholds
import com.example.auroratracker.dto.AuroraPointsDto
import com.example.auroratracker.dto.KpIndexDto
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Service
class TrackingService(
      private val jsonService: JsonService,
      private val userService: UserService,
      private val emailService: EmailService
) {
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()

      fun distanceBetweenUsersAndAuroraPoint(
            auroraLat: Double,
            auroraLon: Double,
            userLat: Double,
            userLon: Double
      ): Double {
            val earthRadius = 6371.0
            val dLat = Math.toRadians(userLat - auroraLat)
            val dLon = Math.toRadians(userLon - auroraLon)

            val a = sin(dLat / 2) * sin(dLat / 2) +
                        cos(Math.toRadians(auroraLat)) * cos(Math.toRadians(userLat)) *
                        sin(dLon / 2) * sin(dLon / 2)

            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return earthRadius * c * 1000
      }

      fun getAuroraPoints(): List<List<Int>> {
            val url = dotenv.get("API_URL_NOAA") ?: ""
            val jsonResponse = jsonService.fetch(url)
            val list = jsonService.parse<AuroraPointsDto>(jsonResponse)
            return list.coordinates ?: emptyList()
      }

      fun getKpIndex(): Int? {
            val url = dotenv.get("API_URL_KP") ?: ""
            val jsonResponse = jsonService.fetch(url)
            val list = jsonService.parse<List<KpIndexDto>>(jsonResponse)
            return list.lastOrNull()?.kpIndex ?:  0
      }

      @Scheduled(fixedDelay = 2700000)
      suspend fun checkAuroraForUsers() {
            val points = getAuroraPoints()
            val kp = getKpIndex() ?: return
            val users = userService.getAllUsers()

            for (user in users) {
                  if (!userService.isAfterSunsetAndClearSky(user)) continue
                  if (userService.hasUserReceivedNotificationRecently(user)) continue

                  val nearby = points.filter { p ->
                        distanceBetweenUsersAndAuroraPoint(user.lat, user.lon, p[1].toDouble(), p[0].toDouble()) <= 1_200_000.0
                  }

                  val shouldNotify = nearby.any { point ->
                        val auroraLon = point[0].toDouble()
                        val auroraLat = point[1].toDouble()
                        val probability = point[2]

                        shouldNotifyUser(user.lat, user.lon, auroraLat, auroraLon, probability, kp)
                  }

                  if (shouldNotify) {
                        val success = emailService.sendEmailAsync(user.email ?: "").await()
                        if (success) {
                              user.lastNotificationTime = LocalDateTime.now()
                              userService.updateLastNotificationTime(user)
                        }
                  }
            }
      }

      fun auroraElevation(userLat: Double, userLon: Double, auroraLat: Double, auroraLon: Double, auroraHeight: Double = 150000.0): Double {
            val distance = distanceBetweenUsersAndAuroraPoint(userLat, userLon, auroraLat, auroraLon)
            return Math.toDegrees(atan(auroraHeight / distance))
      }

      fun getThresholdsForLatitude(lat: Double): Thresholds {
            return when {
                  lat >= 65 -> Thresholds( 2, 300_000.0)
                  lat >= 60 -> Thresholds(4, 500_000.0)
                  else -> Thresholds(5, 600_000.0)
            }
      }

      fun shouldNotifyUser(userLat: Double, userLon: Double, auroraLat: Double, auroraLon: Double, probability: Int, kp: Int): Boolean {
            val thresholds = getThresholdsForLatitude(userLat)
            val distance = distanceBetweenUsersAndAuroraPoint(userLat, userLon, auroraLat, auroraLon)
            if(distance > thresholds.maxDistance) return false

            val elevation = auroraElevation(userLat, userLon, auroraLat, auroraLon)
            if (elevation < 15) return false

            val minProb = minProbForLat(userLat, kp)

            return probability >= minProb && kp >= thresholds.minKp
      }

      fun minProbForLat(lat: Double, kp: Int): Int {
            var base = when {
                  lat >= 65 -> 20
                  lat >= 60 -> 30
                  else -> 40
            }

            if (kp >= 6) base -= 10
            return base.coerceAtLeast(15)
      }
}