package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.dto.SubscriptionDto
import io.mockk.*
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Utils
import org.apache.http.HttpVersion
import org.apache.http.message.BasicHttpResponse
import org.bouncycastle.jce.interfaces.ECPrivateKey
import org.bouncycastle.jce.interfaces.ECPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import java.io.IOException
import java.security.KeyPairGenerator
import java.security.Security
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class WebPushInvalidSubscriptionTest {
    private val env = mockk<EnvConfig>()
    private val subscriptions = mockk<SubscriptionService>(relaxed = true)
    private lateinit var service: WebPushService
    private lateinit var sub: SubscriptionDto

    @BeforeEach
    fun setup() {
        Security.addProvider(BouncyCastleProvider())
        val keys = KeyPairGenerator.getInstance("ECDH", "BC").apply { initialize(ECGenParameterSpec("secp256r1")) }.generateKeyPair()
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val publicKey = encoder.encodeToString(Utils.encode(keys.public as ECPublicKey))
        val privateKey = encoder.encodeToString(Utils.encode(keys.private as ECPrivateKey))
        every { env.vapid } returns EnvConfig.VapidConfig("mailto:test@example.com", publicKey, privateKey)
        sub = SubscriptionDto(userId = "owner", endpoint = "https://web.push.apple.com/expired", p256dh = publicKey,
            auth = encoder.encodeToString(ByteArray(16)))
        mockkConstructor(PushService::class)
        service = WebPushService(env, subscriptions)
    }

    @AfterEach
    fun cleanup() { unmockkConstructor(PushService::class) }

    @ParameterizedTest
    @ValueSource(ints = [404, 410])
    fun `gone endpoints trigger cleanup`(status: Int) {
        every { anyConstructed<PushService>().send(any<Notification>()) } returns BasicHttpResponse(HttpVersion.HTTP_1_1, status, "Gone")
        val delivered = service.sendNotification(sub.endpoint, sub.p256dh, sub.auth, "10", sub)
        assertFalse(delivered)
        verify(exactly = 1) { subscriptions.handleInvalidPushSubscription(sub) }
        verify(exactly = 1) { anyConstructed<PushService>().send(any<Notification>()) }
    }

    @ParameterizedTest
    @ValueSource(ints = [200, 201, 202, 400, 401, 403, 429, 500, 503])
    fun `successes and non-expiry errors preserve subscriptions`(status: Int) {
        every { anyConstructed<PushService>().send(any<Notification>()) } returns BasicHttpResponse(HttpVersion.HTTP_1_1, status, "Response")
        val delivered = service.sendNotification(sub.endpoint, sub.p256dh, sub.auth, "10", sub)
        verify(exactly = 1) { anyConstructed<PushService>().send(any<Notification>()) }
        assertEquals(status in 200..299, delivered)
        verify(exactly = 0) { subscriptions.handleInvalidPushSubscription(any()) }
    }

    @Test
    fun `retaining email subscription after expiry does not count as push delivery`() {
        sub.email = "owner@example.com"
        every { subscriptions.handleInvalidPushSubscription(sub) } returns true
        every { anyConstructed<PushService>().send(any<Notification>()) } returns BasicHttpResponse(HttpVersion.HTTP_1_1, 410, "Gone")
        assertFalse(service.sendNotification(sub.endpoint, sub.p256dh, sub.auth, "10", sub))
        verify(exactly = 1) { subscriptions.handleInvalidPushSubscription(sub) }
    }

    @Test
    fun `network errors never delete subscriptions`() {
        every { anyConstructed<PushService>().send(any<Notification>()) } throws IOException("offline")
        val delivered = service.sendNotification(sub.endpoint, sub.p256dh, sub.auth, "10", sub)
        verify(exactly = 1) { anyConstructed<PushService>().send(any<Notification>()) }
        assertFalse(delivered)
        verify(exactly = 0) { subscriptions.handleInvalidPushSubscription(any()) }
    }
}
