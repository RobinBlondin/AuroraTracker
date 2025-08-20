package com.example.auroratracker.service

import com.example.auroratracker.Thresholds
import com.example.auroratracker.dto.AuroraPointsDto
import com.example.auroratracker.dto.KpIndexDto
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.stereotype.Service
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
      private var auroraPoints: List<List<Int>> = mutableListOf()
      private val distanceLimit = 10.0

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

//      @Scheduled(fixedRate = 60000 * 60)
//      suspend fun sendNotificationToUsers() {
//            val points = getAuroraPoints()
//            val users = userService.getAllUsers()
//
//            for (user in users) {
//                  val isClose = points.any { p ->
//                        val auroraLon = p[0].toDouble()
//                        val auroraLat = p[1].toDouble()
//                        isUserCloseEnoughToAuroraPoint(
//                              auroraLat, auroraLon,
//                              user.lat, user.lon,
//                              distanceLimit
//                        )
//                  }
//                  if (!isClose) continue
//                  if (userService.hasUserReceivedNotificationRecently(user)) continue
//                  if (!userService.isAfterSunsetAndClearSky(user)) continue
//
//                  val success = emailService.sendEmailAsync(user.email ?: "").await()
//                  if (success) {
//                        user.lastNotificationTime = LocalDateTime.now()
//                        userService.updateLastNotificationTime(user)
//                  }
//            }
//      }

      fun auroraElevation(userLat: Double, userLon: Double, auroraLat: Double, auroraLon: Double, auroraHeight: Double = 150000.0): Double {
            val distance = distanceBetweenUsersAndAuroraPoint(userLat, userLon, auroraLat, auroraLon)
            return Math.toDegrees(Math.atan(auroraHeight / distance)) // elevation in degrees
      }

      fun getThresholdsForLatitude(lat: Double): Thresholds {
            return when {
                  lat >= 65 -> Thresholds(20, 2, 300_000.0)    // meters
                  lat >= 60 -> Thresholds(40, 4, 500_000.0)
                  else -> Thresholds(60, 5, 600_000.0)
            }
      }

      fun shouldNotifyUser(userLat: Double, userLon: Double, auroraLat: Double, auroraLon: Double, probability: Int, kp: Int): Boolean {
            val thresholds = getThresholdsForLatitude(userLat)
            val distance = distanceBetweenUsersAndAuroraPoint(userLat, userLon, auroraLat, auroraLon)
            val elevation = auroraElevation(userLat, userLon, auroraLat, auroraLon)

            return probability >= thresholds.minProbability &&
                        kp >= thresholds.minKp &&
                        distance <= thresholds.maxDistance &&
                        elevation >= 15 // degrees
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