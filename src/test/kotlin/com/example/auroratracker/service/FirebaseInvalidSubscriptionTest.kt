package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.dto.SubscriptionDto
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import io.mockk.*
import kotlin.test.assertFalse
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class FirebaseInvalidSubscriptionTest {
    private val env = mockk<EnvConfig>()
    private val subscriptions = mockk<SubscriptionService>(relaxed = true)
    private val messaging = mockk<FirebaseMessaging>()
    private val sub = SubscriptionDto(userId = "owner", firebaseToken = "expired-token")
    private lateinit var service: FirebaseService

    @BeforeEach
    fun setup() {
        every { env.firebase } returns EnvConfig.FirebaseConfig("{}", "vapid", "api", "auth", "project", "bucket", "sender", "app", "measurement")
        mockkStatic(GoogleCredentials::class, FirebaseApp::class, FirebaseMessaging::class)
        every { GoogleCredentials.fromStream(any()) } returns mockk(relaxed = true)
        every { FirebaseApp.getApps() } returns listOf(mockk())
        every { FirebaseMessaging.getInstance() } returns messaging
        service = FirebaseService(env, subscriptions)
    }

    @AfterEach
    fun cleanup() { unmockkStatic(GoogleCredentials::class, FirebaseApp::class, FirebaseMessaging::class) }

    @Test
    fun `unregistered token triggers cleanup`() {
        val error = mockk<FirebaseMessagingException>()
        every { error.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
        every { messaging.send(any<Message>()) } throws error
        service.sendNotification("expired-token", 10.0, sub)
        verify(exactly = 1) { subscriptions.handleInvalidPushSubscription(sub) }
    }

    @ParameterizedTest
    @EnumSource(value = MessagingErrorCode::class, names = ["UNREGISTERED"], mode = EnumSource.Mode.EXCLUDE)
    fun `other firebase error codes preserve subscriptions`(code: MessagingErrorCode) {
        val error = mockk<FirebaseMessagingException>(relaxed = true)
        every { error.messagingErrorCode } returns code
        every { messaging.send(any<Message>()) } throws error
        service.sendNotification("expired-token", 10.0, sub)
        verify(exactly = 0) { subscriptions.handleInvalidPushSubscription(any()) }
    }

    @Test
    fun `retaining email subscription after unregistration does not count as push delivery`() {
        sub.email = "owner@example.com"
        val error = mockk<FirebaseMessagingException>()
        every { error.messagingErrorCode } returns MessagingErrorCode.UNREGISTERED
        every { messaging.send(any<Message>()) } throws error
        every { subscriptions.handleInvalidPushSubscription(sub) } returns true
        assertFalse(service.sendNotification("expired-token", 10.0, sub))
        verify(exactly = 1) { subscriptions.handleInvalidPushSubscription(sub) }
    }

    @Test
    fun `successful firebase delivery never deletes a subscription`() {
        every { messaging.send(any<Message>()) } returns "message-id"
        service.sendNotification("expired-token", 10.0, sub)
        verify(exactly = 1) { messaging.send(any<Message>()) }
        verify(exactly = 0) { subscriptions.handleInvalidPushSubscription(any()) }
    }
}
