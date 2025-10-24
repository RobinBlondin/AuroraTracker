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
import kotlin.math.*

@Service
class TrackingService(
      private val jsonService: JsonService,
      private val subscriptionService: SubscriptionService,
) {
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()
      private val log = LoggerFactory.getLogger(this::class.java)

      companion object {
            const val MAX_DISTANCE_FROM_AURORA_METERS = 600_000.0
            const val ONE_MIL_IN_METERS  = 10_000.0
            const val EARTH_RADIUS_KM = 6371.0
      }

      /**
       * Calculates the great-circle distance between two latitude/longitude points
       * using the Haversine formula.
       * Returns the distance in meters.
       *
       * Reference: https://en.wikipedia.org/wiki/Haversine_formula
       */
      fun distanceBetweenUsersAndAuroraPointInMeters(
            auroraLat: Double,
            auroraLon: Double,
            userLat: Double,
            userLon: Double
      ): Double {
            val dLat = Math.toRadians(userLat - auroraLat)
            val dLon = Math.toRadians(userLon - auroraLon)

            val a = sin(dLat / 2) * sin(dLat / 2) +
                        cos(Math.toRadians(auroraLat)) * cos(Math.toRadians(userLat)) *
                        sin(dLon / 2) * sin(dLon / 2)

            val c = 2 * atan2(sqrt(a), sqrt(1 - a))

            val distanceKm = EARTH_RADIUS_KM * c
            val distanceMeters = distanceKm * 1000
            return distanceMeters
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
            val subs = subscriptionService.getAllSubs()
            log.info("KpIndex: $kp at ${LocalDateTime.now().toString().split("T")[1].take(5)}")

            for (sub in subs) {
                  if (!subscriptionService.isAfterSunsetAndClearSky(sub)) continue
                  if (subscriptionService.hasUserReceivedNotificationRecently(sub)) continue

                  val nearby = points.filter { p ->
                        distanceBetweenUsersAndAuroraPointInMeters(sub.lat!!, sub.lon!!, p.lat , p.lon) <= MAX_DISTANCE_FROM_AURORA_METERS
                  }

                  val shouldNotify = nearby.any { point ->
                        val auroraLon = point.lon
                        val auroraLat = point.lat
                        val probability = point.probability

                        shouldNotifyUser(sub.lat!!, sub.lon!!, auroraLat, auroraLon, probability, kp)
                  }

                  if (shouldNotify) {
                       TODO("Send push notification to user")
                  }
            }
      }

      fun auroraElevation(userLat: Double, userLon: Double, auroraLat: Double, auroraLon: Double, auroraHeight: Double = 150000.0): Double {
            val distance = distanceBetweenUsersAndAuroraPointInMeters(userLat, userLon, auroraLat, auroraLon)
            return Math.toDegrees(atan(auroraHeight / distance))
      }

      fun getThresholdsForLatitude(lat: Double, kp: Int): Thresholds {
            val minKp = when {
                  lat >= 65 -> 2
                  lat >= 60 -> 4
                  else -> 5
            }

            val maxDistance = when {
                  lat >= 65 -> 30 * ONE_MIL_IN_METERS
                  lat >= 60 -> 50 * ONE_MIL_IN_METERS
                  else -> 60 * ONE_MIL_IN_METERS
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
            val distance = distanceBetweenUsersAndAuroraPointInMeters(userLat, userLon, auroraLat, auroraLon)
            if(distance > thresholds.maxDistance) return false

            val elevation = auroraElevation(userLat, userLon, auroraLat, auroraLon)
            if (elevation < 15) return false

            return probability >= thresholds.minProbability && kp >= thresholds.minKp
      }
}