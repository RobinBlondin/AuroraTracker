package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.dto.AuroraPointDto
import com.example.auroratracker.dto.SubscriptionDto
import com.example.auroratracker.dto.WeatherResponseDto
import com.example.auroratracker.enums.NotificationLevel
import io.mockk.*
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.IOException
import java.time.ZoneOffset
import java.time.ZonedDateTime
import kotlin.test.*

class TrackingServiceTest {
    private val json = spyk(JsonService(mockk()))
    private val subscriptions = mockk<SubscriptionService>()
    private val points = mockk<AuroraPointService>()
    private val webPush = mockk<WebPushService>(relaxed = true)
    private val firebase = mockk<FirebaseService>(relaxed = true)
    private val history = mockk<NotificationService>(relaxed = true)
    private val email = mockk<EmailService>(relaxed = true)
    private val env = mockk<EnvConfig>()
    private val realSubscriptionHelpers = SubscriptionService(mockk(), mockk(), mockk(), mockk())
    private val service = TrackingService(json, subscriptions, points, webPush, firebase, history, email, env)
    private val kpUrl = "https://example.test/kp"
    private val sub = SubscriptionDto(userId = "user", lat = 60.0, lon = 18.0,
        firebaseToken = "fcm-token", enabled = true, notificationLevel = NotificationLevel.ALL)

    private fun weather(cloudCover: Double = 10.0, time: String = "21:00") = Json.decodeFromString<WeatherResponseDto>(
        """{"current":{"time":"2026-10-30T$time","cloud_cover":$cloudCover},"daily":{"sunset":["2026-10-30T18:00"]}}"""
    )

    @BeforeEach
    fun setup() {
        every { env.api } returns EnvConfig.ApiConfig(EnvConfig.ApiConfig.Urls("unused", kpUrl, "unused"))
        every { json.fetch(kpUrl) } returns """[{"kp_index":4}]"""
        every { points.getAuroraPoints() } returns listOf(AuroraPointDto(18.0, 60.5, 80.0))
        every { subscriptions.getAllSubs() } returns listOf(sub)
        every { subscriptions.getWeatherData(any()) } returns weather()
        every { subscriptions.isAfterSunset(any()) } answers { realSubscriptionHelpers.isAfterSunset(firstArg()) }
        every { subscriptions.hasReceivedNotificationRecently(any()) } answers {
            realSubscriptionHelpers.hasReceivedNotificationRecently(firstArg())
        }
        every { subscriptions.updateLastNotificationTime(any()) } answers { firstArg() }
        every { firebase.sendNotification(any(), any(), any()) } returns true
        every { webPush.sendNotification(any(), any(), any(), any(), any()) } returns true
    }

    private fun assertNoDelivery() {
        verify { email wasNot Called; firebase wasNot Called; webPush wasNot Called; history wasNot Called }
        verify(exactly = 0) { subscriptions.updateLastNotificationTime(any()) }
    }

    @Test
    fun `eligible firebase user gets one notification and history entry`() {
        service.checkAuroraForUsers()
        verify(exactly = 1) { firebase.sendNotification("fcm-token", 10.0, sub) }
        verify(exactly = 1) { subscriptions.updateLastNotificationTime(sub) }
        verify(exactly = 1) { history.add(match { it.userId == "user" && it.lat == 60.0 && it.lon == 18.0 }) }
        verify { email wasNot Called; webPush wasNot Called }
    }

    @Test
    fun `eligible web push user gets push with stored credentials`() {
        sub.firebaseToken = null
        sub.endpoint = "endpoint"; sub.p256dh = "public-key"; sub.auth = "auth-key"
        service.checkAuroraForUsers()
        verify(exactly = 1) { webPush.sendNotification("endpoint", "public-key", "auth-key", "10.0", sub) }
        verify { firebase wasNot Called; email wasNot Called }
    }

    @Test
    fun `email and firebase subscription receives both channels`() {
        sub.email = "user@example.com"
        service.checkAuroraForUsers()
        verify(exactly = 1) { email.sendNotificationEmail("user@example.com") }
        verify(exactly = 1) { firebase.sendNotification("fcm-token", 10.0, sub) }
        verify(exactly = 1) { subscriptions.updateLastNotificationTime(sub) }
    }

    @Test
    fun `email and web push subscription receives both channels`() {
        sub.email = "user@example.com"; sub.firebaseToken = null
        sub.endpoint = "endpoint"; sub.p256dh = "key"; sub.auth = "auth"
        service.checkAuroraForUsers()
        verify(exactly = 1) { email.sendNotificationEmail("user@example.com") }
        verify(exactly = 1) { webPush.sendNotification("endpoint", "key", "auth", "10.0", sub) }
    }

    @Test
    fun `email only subscription never attempts web push`() {
        sub.email = "user@example.com"; sub.firebaseToken = null
        service.checkAuroraForUsers()
        verify(exactly = 1) { email.sendNotificationEmail("user@example.com") }
        verify { webPush wasNot Called; firebase wasNot Called }
    }

    @Test
    fun `missing delivery channels do not consume notification cooldown`() {
        sub.firebaseToken = null
        service.checkAuroraForUsers()
        assertNoDelivery()
    }

    @Test
    fun `invalid legacy subscription does not prevent notifying the next user`() {
        val invalid = SubscriptionDto(userId = "legacy", lat = null, lon = null, enabled = true)
        every { subscriptions.getAllSubs() } returns listOf(invalid, sub)
        service.checkAuroraForUsers()
        verify(exactly = 1) { firebase.sendNotification("fcm-token", 10.0, sub) }
        verify(exactly = 0) { subscriptions.updateLastNotificationTime(invalid) }
    }

    @Test
    fun `one user delivery exception does not abort all remaining users`() {
        val broken = SubscriptionDto(userId = "broken", lat = 60.0, lon = 18.0, email = "broken@example.com", enabled = true)
        every { subscriptions.getAllSubs() } returns listOf(broken, sub)
        every { email.sendNotificationEmail("broken@example.com") } throws IllegalStateException("delivery failure")
        service.checkAuroraForUsers()
        verify(exactly = 1) { firebase.sendNotification("fcm-token", 10.0, sub) }
        verify(exactly = 0) { subscriptions.updateLastNotificationTime(broken) }
    }

    @Test
    fun `invalid push cleanup must not be followed by saving the deleted subscription`() {
        every { firebase.sendNotification("fcm-token", 10.0, sub) } answers {
            subscriptions.handleInvalidPushSubscription(sub)
        }
        every { subscriptions.handleInvalidPushSubscription(sub) } returns false
        service.checkAuroraForUsers()
        verify(exactly = 1) { subscriptions.handleInvalidPushSubscription(sub) }
        verify(exactly = 0) { subscriptions.updateLastNotificationTime(sub) }
        verify { history wasNot Called }
    }

    @Test
    fun `successful email still records delivery when push fails`() {
        sub.email = "user@example.com"
        every { firebase.sendNotification("fcm-token", 10.0, sub) } returns false
        service.checkAuroraForUsers()
        verify(exactly = 1) { email.sendNotificationEmail("user@example.com") }
        verify(exactly = 1) { subscriptions.updateLastNotificationTime(sub) }
        verify(exactly = 1) { history.add(any()) }
    }

    @Test
    fun `failed push does not consume cooldown`() {
        every { firebase.sendNotification("fcm-token", 10.0, sub) } returns false
        service.checkAuroraForUsers()
        verify(exactly = 0) { subscriptions.updateLastNotificationTime(any()) }
        verify { history wasNot Called }
    }

    @Test
    fun `empty aurora data skips all subscriptions and remote kp fetch`() {
        every { points.getAuroraPoints() } returns emptyList()
        service.checkAuroraForUsers()
        assertNoDelivery()
        verify(exactly = 0) { json.fetch(any()) }
        verify(exactly = 0) { subscriptions.getAllSubs() }
    }

    @Test
    fun `unavailable kp data skips notifications`() {
        every { json.fetch(kpUrl) } throws IOException("offline")
        service.checkAuroraForUsers()
        assertNoDelivery()
        verify(exactly = 0) { subscriptions.getAllSubs() }
    }

    @Test
    fun `disabled subscriptions are skipped before requesting weather`() {
        sub.enabled = false
        service.checkAuroraForUsers()
        assertNoDelivery()
        verify(exactly = 0) { subscriptions.getWeatherData(any()) }
    }

    @Test
    fun `daylight skips all delivery channels`() {
        every { subscriptions.getWeatherData(sub) } returns weather(time = "12:00")
        service.checkAuroraForUsers()
        assertNoDelivery()
    }

    @Test
    fun `recently notified subscriptions are skipped`() {
        sub.lastNotificationTime = ZonedDateTime.now(ZoneOffset.UTC).minusHours(1)
        service.checkAuroraForUsers()
        assertNoDelivery()
    }

    @Test
    fun `subscriptions older than the twelve hour cooldown can be notified`() {
        sub.lastNotificationTime = ZonedDateTime.now(ZoneOffset.UTC).minusHours(13)
        service.checkAuroraForUsers()
        verify(exactly = 1) { firebase.sendNotification("fcm-token", 10.0, sub) }
    }

    @Test
    fun `distant aurora points do not trigger notifications`() {
        every { points.getAuroraPoints() } returns listOf(AuroraPointDto(18.0, 70.0, 100.0))
        service.checkAuroraForUsers()
        assertNoDelivery()
    }

    @Test
    fun `multiple nearby aurora points only produce one notification`() {
        every { points.getAuroraPoints() } returns listOf(AuroraPointDto(18.0, 60.5, 80.0), AuroraPointDto(18.0, 60.8, 90.0))
        service.checkAuroraForUsers()
        verify(exactly = 1) { firebase.sendNotification("fcm-token", 10.0, sub) }
        verify(exactly = 1) { history.add(any()) }
    }

    @ParameterizedTest
    @CsvSource("ALL,100,true", "CLEAR,30,true", "CLEAR,30.1,false", "CLEAR_AND_STRONG,15,true", "CLEAR_AND_STRONG,15.1,false")
    fun `scheduled checks respect cloud preferences`(level: NotificationLevel, clouds: Double, expected: Boolean) {
        sub.notificationLevel = level
        every { subscriptions.getWeatherData(sub) } returns weather(clouds)
        service.checkAuroraForUsers()
        if (expected) verify(exactly = 1) { firebase.sendNotification("fcm-token", clouds, sub) } else assertNoDelivery()
    }

    @Test
    fun `weather outage suppresses clear sky subscriptions`() {
        sub.notificationLevel = NotificationLevel.CLEAR
        every { subscriptions.getWeatherData(sub) } returns null
        service.checkAuroraForUsers()
        assertNoDelivery()
    }

    @Test
    fun `weather outage currently permits ALL subscriptions without a sunset check`() {
        every { subscriptions.getWeatherData(sub) } returns null
        service.checkAuroraForUsers()
        verify(exactly = 1) { firebase.sendNotification("fcm-token", 100.0, sub) }
    }

    @Test
    fun `kp current uses latest valid value rather than maximum`() {
        every { json.fetch(kpUrl) } returns """[{"kp_index":7},{"kp_index":3},{}]"""
        assertEquals(3, service.getKpIndex(current = true))
        assertEquals(7, service.getKpIndex())
    }

    @Test
    fun `kp maximum ignores entries outside the last sixty readings`() {
        every { json.fetch(kpUrl) } returns (listOf("{\"kp_index\":9}") + List(60) { "{\"kp_index\":3}" }).joinToString(",", "[", "]")
        assertEquals(3, service.getKpIndex())
    }

    @Test
    fun `missing and malformed kp data returns no index`() {
        for (body in listOf("[]", "[{}]", "not json")) {
            every { json.fetch(kpUrl) } returns body
            assertNull(service.getKpIndex())
        }
    }

    @ParameterizedTest
    @CsvSource("ALL,3,30,100,true", "ALL,2,100,0,false", "ALL,3,29,0,false", "CLEAR,3,30,30,true", "CLEAR,3,30,30.01,false", "CLEAR_AND_STRONG,3,100,0,false", "CLEAR_AND_STRONG,4,45,15,true", "CLEAR_AND_STRONG,4,44,15,false", "CLEAR_AND_STRONG,4,45,15.01,false")
    fun `decision respects minimum kp probability and cloud boundaries`(level: NotificationLevel, kp: Int, probability: Double, clouds: Double, expected: Boolean) {
        assertEquals(expected, service.shouldNotifyUser(60.0, 18.0, 60.5, 18.0, probability, kp, level, clouds))
    }

    @Test
    fun `strong preference rejects points that are close enough but too low in the sky`() {
        assertTrue(service.shouldNotifyUser(60.0, 18.0, 63.0, 18.0, 90.0, 4, NotificationLevel.ALL, 0.0))
        assertFalse(service.shouldNotifyUser(60.0, 18.0, 63.0, 18.0, 90.0, 4, NotificationLevel.CLEAR_AND_STRONG, 0.0))
    }

    @Test
    fun `latitude specific distance limit still applies inside the initial six hundred km search`() {
        assertFalse(service.shouldNotifyUser(60.0, 18.0, 65.0, 18.0, 100.0, 9, NotificationLevel.ALL, 0.0))
    }

    @Test
    fun `high activity relaxes probability cutoff`() {
        assertFalse(service.shouldNotifyUser(60.0, 18.0, 60.5, 18.0, 20.0, 5, NotificationLevel.ALL, 0.0))
        assertTrue(service.shouldNotifyUser(60.0, 18.0, 60.5, 18.0, 20.0, 6, NotificationLevel.ALL, 0.0))
    }

    @Test
    fun `latitude thresholds are symmetric across hemispheres`() {
        for (latitude in listOf(55.0, 60.0, 65.0)) {
            for (level in NotificationLevel.entries) {
                assertEquals(service.getThresholdsForLatitude(latitude, 4, level), service.getThresholdsForLatitude(-latitude, 4, level))
            }
        }
    }

    @Test
    fun `distance and elevation calculations match known geometry`() {
        assertEquals(0.0, service.distanceBetweenUsersAndAuroraPointInMeters(60.0, 18.0, 60.0, 18.0), 0.0001)
        assertEquals(111194.93, service.distanceBetweenUsersAndAuroraPointInMeters(60.0, 18.0, 61.0, 18.0), 0.1)
        assertEquals(90.0, service.auroraElevation(60.0, 18.0, 60.0, 18.0), 0.0001)
    }
}
