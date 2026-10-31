package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.domain.Thresholds
import com.example.auroratracker.dto.KpIndexDto
import com.example.auroratracker.entity.Notification
import com.example.auroratracker.enums.NotificationLevel
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
    private val auroraPointService: AuroraPointService,
    private val webPushService: WebPushService,
    private val firebaseService: FirebaseService,
    private val notificationService: NotificationService,
    private val emailService: EmailService,
    private val env: EnvConfig
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    companion object {
        const val MAX_DISTANCE_FROM_AURORA_METERS = 600_000.0
        const val ONE_MIL_IN_METERS = 10_000.0
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


    fun getKpIndex(current: Boolean = false): Int? {
        val url = env.api.url.kp
        val response = jsonService.fetchAndParse<List<KpIndexDto>>(url)
        val result = response.getOrElse {
            println("Failed to fetch or parse Kp index: ${it.message}")
            emptyList()
        }

        if (result.isEmpty()) {
            return null
        }

        if (current) return result.lastOrNull { it.kpIndex != null }?.kpIndex

        val indexesOfLastHour = result.takeLast(60).mapNotNull { it.kpIndex }

        return indexesOfLastHour.maxOrNull()
    }

    @Scheduled(cron = "0 1,31 * * * *")
    fun checkAuroraForUsers() = runBlocking {
        val points = auroraPointService.getAuroraPoints()
        if (points.isEmpty()) return@runBlocking

        val kp = getKpIndex() ?: return@runBlocking
        val subs = subscriptionService.getAllSubs()
        log.info("KpIndex: $kp at ${LocalDateTime.now().toString().split("T")[1].take(5)}")

        for (sub in subs) {
            if (sub.enabled != true) continue
            val weatherData = subscriptionService.getWeatherData(sub)
            val cloudCover = weatherData?.current?.cloudCover ?: 100.0
            if (weatherData != null && !subscriptionService.isAfterSunset(weatherData)) continue
            if (subscriptionService.hasReceivedNotificationRecently(sub)) continue
            if (sub.lat == null || sub.lon == null) continue

            val nearby = points.filter { p ->
                distanceBetweenUsersAndAuroraPointInMeters(
                    sub.lat!!,
                    sub.lon!!,
                    p.lat,
                    p.lon
                ) <= MAX_DISTANCE_FROM_AURORA_METERS
            }

            val shouldNotify = nearby.any { point ->
                val auroraLon = point.lon
                val auroraLat = point.lat
                val probability = point.probability

                shouldNotifyUser(
                    sub.lat!!,
                    sub.lon!!,
                    auroraLat,
                    auroraLon,
                    probability,
                    kp,
                    sub.notificationLevel!!,
                    cloudCover
                )
            }

            if (shouldNotify) {
                var notified = false
                if (sub.email != null) {
                    try {
                        emailService.sendNotificationEmail(sub.email!!)
                        notified = true
                    } catch (ex: Exception) {
                        println("Failed to send notification email: ${ex.message}")
                    }
                }

                if (sub.firebaseToken != null) {
                    notified = firebaseService.sendNotification(sub.firebaseToken!!, cloudCover, sub) || notified
                } else if (sub.endpoint != null && sub.p256dh != null && sub.auth != null) {
                    notified = webPushService.sendNotification(
                        sub.endpoint,
                        sub.p256dh,
                        sub.auth,
                        cloudCover.toString(),
                        sub
                    ) || notified
                }

                if (notified) {
                    subscriptionService.updateLastNotificationTime(sub)
                    notificationService.add(Notification(userId = sub.userId, lat = sub.lat, lon = sub.lon))
                }
            }
        }
    }

    fun auroraElevation(
        userLat: Double,
        userLon: Double,
        auroraLat: Double,
        auroraLon: Double,
        auroraHeight: Double = 150000.0
    ): Double {
        val distance = distanceBetweenUsersAndAuroraPointInMeters(userLat, userLon, auroraLat, auroraLon)
        return Math.toDegrees(atan(auroraHeight / distance))
    }


    fun getThresholdsForLatitude(
        lat: Double,
        kp: Int,
        level: NotificationLevel
    ): Thresholds {
        val latitude = kotlin.math.abs(lat)

        val baseKp = when {
            latitude >= 65 -> 2
            latitude >= 60 -> 3
            else -> 4
        }

        val baseDistance = when {
            latitude >= 65 -> 30
            latitude >= 60 -> 50
            else -> 60
        }

        val baseProbability = when {
            latitude >= 65 -> 20
            latitude >= 60 -> 30
            else -> 40
        }

        val isStrong = level == NotificationLevel.CLEAR_AND_STRONG

        val minKp = baseKp + if (isStrong) 1 else 0

        val maxDistance = baseDistance *
                (if (isStrong) 0.7 else 1.0) *
                ONE_MIL_IN_METERS

        val minProbability = (
                baseProbability +
                        (if (isStrong) 15 else 0) -
                        (if (kp >= 6) 10 else 0)
                ).coerceIn(15, 100)

        val maxCloudCover = when (level) {
            NotificationLevel.ALL -> 100.0
            NotificationLevel.CLEAR -> 30.0
            NotificationLevel.CLEAR_AND_STRONG -> 15.0
        }

        return Thresholds(
            minKp = minKp,
            maxDistance = maxDistance,
            minProbability = minProbability,
            maxCloudCover = maxCloudCover,
            minElevation = if (isStrong) 25.0 else 15.0
        )
    }

    fun shouldNotifyUser(
        userLat: Double,
        userLon: Double,
        auroraLat: Double,
        auroraLon: Double,
        probability: Double,
        kp: Int,
        level: NotificationLevel,
        cloudCover: Double
    ): Boolean {
        val thresholds = getThresholdsForLatitude(
            userLat, kp, level
        )

        if (kp < thresholds.minKp) return false
        if (probability < thresholds.minProbability) return false
        if (cloudCover > thresholds.maxCloudCover) return false

        val distance = distanceBetweenUsersAndAuroraPointInMeters(
            userLat, userLon, auroraLat, auroraLon
        )

        if (distance > thresholds.maxDistance) return false

        val elevation = auroraElevation(
            userLat, userLon, auroraLat, auroraLon
        )

        if (elevation < thresholds.minElevation) return false

        return true
    }

}