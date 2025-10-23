package com.example.auroratracker.service

import com.example.auroratracker.domain.Thresholds
import com.example.auroratracker.dto.AuroraBelt
import com.example.auroratracker.dto.AuroraPoint
import com.example.auroratracker.dto.KpIndexDto
import io.github.cdimascio.dotenv.Dotenv
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.math.*

@Service
class TrackingService(
      private val jsonService: JsonService,
      private val userService: UserService,
      private val emailService: EmailService
) {
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()
      private val log = LoggerFactory.getLogger(this::class.java)

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

      fun getAuroraPoints(): List<AuroraPoint> {
            val url = dotenv.get("API_URL_NOAA") ?: ""
            val response = jsonService.fetchAndParse<AuroraBelt>(url)
            val auroraBelt = response.getOrElse {
                  println("Failed to fetch or parse aurora points: ${it.message}")
                  AuroraBelt()
            }
            return auroraBelt.convertToAuroraPoints()
      }

      fun getKpIndex(): Int? {
            val url = dotenv.get("API_URL_KP") ?: ""
            val response = jsonService.fetchAndParse<List<KpIndexDto>>(url)
            val result = response.getOrElse {
                  println("Failed to fetch or parse Kp index: ${it.message}")
                  emptyList()
            }

            if (result.isEmpty()) {
                  return null
            }

            val indexesOfLastHour = result.takeLast(60).mapNotNull { it.kpIndex }

            return indexesOfLastHour.maxOrNull()
      }

      @Scheduled(fixedDelay = 1800000)
       fun checkAuroraForUsers() = runBlocking {
            val points = getAuroraPoints()
            if (points.isEmpty()) return@runBlocking

            val kp = getKpIndex() ?: return@runBlocking
            val users = userService.getAllUsers()
            log.info("KpIndex: $kp at ${LocalDateTime.now().toString().split("T")[1].take(5)}")

            for (user in users) {
                  if (!userService.isAfterSunsetAndClearSky(user)) continue
                  if (userService.hasUserReceivedNotificationRecently(user)) continue

                  val nearby = points.filter { p ->
                        distanceBetweenUsersAndAuroraPoint(user.lat, user.lon, p.lat , p.lon) <= 1_200_000.0
                  }

                  val shouldNotify = nearby.any { point ->
                        val auroraLon = point.lon
                        val auroraLat = point.lat
                        val probability = point.probability

                        shouldNotifyUser(user.lat, user.lon, auroraLat, auroraLon, probability, kp)
                  }

                  if (shouldNotify) {
                        log.info("Email sent to user: ${user.name} at (${user.lat.toString().take(5)}, ${user.lon.toString().take(5)})")

                        val success = emailService.sendEmailAsync(user, "Aurora Tracker Notification", "notification.html", true, kp).await()
                        if (success) {
                              user.lastNotificationTime = ZonedDateTime.now(ZoneOffset.UTC)
                              userService.updateLastNotificationTime(user)
                        }
                  }
            }
      }

      fun auroraElevation(userLat: Double, userLon: Double, auroraLat: Double, auroraLon: Double, auroraHeight: Double = 150000.0): Double {
            val distance = distanceBetweenUsersAndAuroraPoint(userLat, userLon, auroraLat, auroraLon)
            return Math.toDegrees(atan(auroraHeight / distance))
      }

      fun getThresholdsForLatitude(lat: Double, kp: Int): Thresholds {
            val minKp = when {
                  lat >= 65 -> 2
                  lat >= 60 -> 4
                  else -> 5
            }

            val maxDistance = when {
                  lat >= 65 -> 300_000.0
                  lat >= 60 -> 500_000.0
                  else -> 600_000.0
            }

            val baseProbability = when {
                  lat >= 65 -> 20
                  lat >= 60 -> 30
                  else -> 40
            }

            val minProbability = (baseProbability - if (kp >= 6) 10 else 0).coerceAtLeast(15)

            return Thresholds(minKp = minKp, maxDistance = maxDistance, minProbability = minProbability)
      }

      fun shouldNotifyUser(userLat: Double, userLon: Double, auroraLat: Double, auroraLon: Double, probability: Double, kp: Int): Boolean {
            val thresholds = getThresholdsForLatitude(userLat, kp)
            val distance = distanceBetweenUsersAndAuroraPoint(userLat, userLon, auroraLat, auroraLon)
            if(distance > thresholds.maxDistance) return false

            val elevation = auroraElevation(userLat, userLon, auroraLat, auroraLon)
            if (elevation < 15) return false

            return probability >= thresholds.minProbability && kp >= thresholds.minKp
      }
}