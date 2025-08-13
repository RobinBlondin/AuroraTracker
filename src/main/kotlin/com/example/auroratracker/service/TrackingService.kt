package com.example.auroratracker.service

import com.example.auroratracker.dto.NOAAResponseDto
import com.example.auroratracker.dto.UserDto
import io.github.cdimascio.dotenv.Dotenv
import jakarta.annotation.PostConstruct
import org.springframework.scheduling.annotation.Scheduled
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
      var userMap = mutableMapOf<String, List<UserDto>>()
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()
      private var auroraPoints: List<List<Int>> = mutableListOf()
      private val distanceLimit = 10.0

      @PostConstruct
      fun init() {
            loadUsers()
      }

      @Scheduled(fixedRate = 60000 * 15)
      fun loadUsers() {
            userMap = userService.getAllUsersInLocationMap().toMutableMap()
      }

      fun isUserCloseEnoughToAuroraPoint(
            auroraLat: Double,
            auroraLon: Double,
            userLat: Double,
            userLon: Double,
            limit: Double
      ): Boolean {
            val earthRadius = 6371.0
            val dLat = Math.toRadians(userLat - auroraLat)
            val dLon = Math.toRadians(userLon - auroraLon)

            val a = sin(dLat / 2) * sin(dLat / 2) +
                        cos(Math.toRadians(auroraLat)) * cos(Math.toRadians(userLat)) *
                        sin(dLon / 2) * sin(dLon / 2)

            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            val distance = earthRadius * c
            return distance <= limit
      }

      fun getAuroraPoints(): List<List<Int>> {
            val url = dotenv.get("API_URL_NOAA") ?: ""
            val jsonResponse = jsonService.fetch(url)
            val list = jsonService.parse<NOAAResponseDto>(jsonResponse)
            return list.coordinates!!.filter { it[2] >= 80 }
      }

      @Scheduled(fixedRate = 60000 * 60)
      suspend fun sendNotificationToUsers() {
            auroraPoints = getAuroraPoints()
            userMap.forEach { (location, users) ->
                  val (lat, lon) = location.split(",").map { it.toDouble() }
                  users.forEach { user ->
                        if (
                              isUserCloseEnoughToAuroraPoint(lat, lon, user.lat, user.lon, distanceLimit) &&
                              userService.isAfterSunsetAndClearSky(user) &&
                              !userService.hasUserReceivedNotificationRecently(user)
                        ) {
                              val success = emailService.sendEmailAsync(user.email ?: "").await()
                              if( success ) {
                                    userService.updateLastNotificationTime(user)
                              }
                        }
                  }
            }
      }
}