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

            for (user in users) {
                  log.error("User: ${user.name} at (${user.lat.toString().take(5)}, ${user.lon.toString().take(5)})")
                  log.error("KpIndex: $kp")
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
                        val success = emailService.sendEmailAsync(user.email ?: "").await()
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

      fun getThresholdsForLatitude(lat: Double): Thresholds {
            return when {
                  lat >= 65 -> Thresholds( 2, 300_000.0)
                  lat >= 60 -> Thresholds(4, 500_000.0)
                  else -> Thresholds(5, 600_000.0)
            }
      }

      fun shouldNotifyUser(userLat: Double, userLon: Double, auroraLat: Double, auroraLon: Double, probability: Double, kp: Int): Boolean {
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