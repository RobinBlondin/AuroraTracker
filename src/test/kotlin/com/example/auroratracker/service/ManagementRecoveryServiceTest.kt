package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.entity.ManagementToken
import com.example.auroratracker.entity.Subscription
import com.example.auroratracker.repository.ManagementTokenRepository
import com.example.auroratracker.repository.SubscriptionRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.security.MessageDigest
import java.time.Instant
import kotlin.test.*

class ManagementRecoveryServiceTest {
    private val subscriptions = mockk<SubscriptionRepository>()
    private val tokens = mockk<ManagementTokenRepository>()
    private val email = mockk<EmailService>()
    private val push = mockk<WebPushService>()
    private val firebase = mockk<FirebaseService>()
    private val env = mockk<EnvConfig>()
    private lateinit var service: ManagementRecoveryService
    private val subscription = Subscription(email = "person@example.com", userId = "owner", endpoint = "stored-endpoint", p256dh = "key", auth = "auth")

    @BeforeEach
    fun setup() {
        every { env.resend } returns EnvConfig.ResendConfig("unused", "https://auroratracker.se/verification/")
        every { subscriptions.findForManagementIssuance("owner") } returns subscription
        every { tokens.countByUserIdAndCreatedAtAfter("owner", any()) } returns 0
        every { tokens.save(any()) } answers { firstArg() }
        service = ManagementRecoveryService(subscriptions, tokens, email, push, firebase, env)
    }

    @Test
    fun `email delivers a ten minute link and stores only its hash`() {
        every { subscriptions.findFirstByEmailIgnoreCase("PERSON@example.com") } returns subscription
        val delivered = slot<String>()
        every { email.sendManagementEmail("person@example.com", capture(delivered)) } just Runs
        val record = slot<ManagementToken>()
        every { tokens.save(capture(record)) } answers { firstArg() }
        val before = Instant.now()
        service.requestEmail(" PERSON@example.com ")
        assertTrue(delivered.captured.startsWith("https://auroratracker.se/manage-alerts#token="))
        val token = delivered.captured.substringAfter("#token=")
        assertEquals(43, token.length)
        assertEquals(hash(token), record.captured.tokenHash)
        assertFalse(record.captured.tokenHash.contains(token))
        assertTrue(record.captured.expiresAt >= before.plusSeconds(600))
        assertTrue(record.captured.expiresAt <= Instant.now().plusSeconds(600))
    }

    @Test
    fun `unknown emails and endpoints never send a message`() {
        every { subscriptions.findFirstByEmailIgnoreCase(any()) } returns null
        every { subscriptions.findFirstByEndpoint(any()) } returns null
        service.requestEmail("unknown@example.com")
        service.requestPush("unknown-endpoint", null)
        verify { email wasNot Called; push wasNot Called; firebase wasNot Called }
        verify(exactly = 0) { tokens.save(any()) }
    }

    @Test
    fun `web push uses stored credentials and does not establish a session`() {
        every { subscriptions.findFirstByEndpoint("stored-endpoint") } returns subscription
        every { push.sendManagementNotification("stored-endpoint", "key", "auth", any()) } just Runs
        service.requestPush("stored-endpoint", null)
        verify(exactly = 1) { push.sendManagementNotification("stored-endpoint", "key", "auth", match { it.contains("#token=") }) }
        verify { email wasNot Called; firebase wasNot Called }
    }

    @Test
    fun `firebase subscriptions receive the challenge through firebase`() {
        val fcm = Subscription(email = null, userId = "owner", firebaseToken = "stored-fcm-token")
        every { subscriptions.findFirstByFirebaseToken("stored-fcm-token") } returns fcm
        every { firebase.sendManagementNotification("stored-fcm-token", any()) } just Runs
        service.requestPush(null, "stored-fcm-token")
        verify(exactly = 1) { firebase.sendManagementNotification("stored-fcm-token", any()) }
        verify { push wasNot Called }
    }

    @Test
    fun `subscription cooldown suppresses repeated deliveries`() {
        every { subscriptions.findFirstByEmailIgnoreCase(any()) } returns subscription
        every { tokens.countByUserIdAndCreatedAtAfter("owner", any()) } returns 1
        service.requestEmail("person@example.com")
        verify { email wasNot Called }
        verify(exactly = 0) { tokens.save(any()) }
    }

    @Test
    fun `delivery failure consumes token without exposing provider errors`() {
        every { subscriptions.findFirstByEmailIgnoreCase(any()) } returns subscription
        every { email.sendManagementEmail(any(), any()) } throws IllegalStateException("provider failure")
        val records = mutableListOf<ManagementToken>()
        every { tokens.save(capture(records)) } answers { firstArg() }
        service.requestEmail("person@example.com")
        assertTrue(records.last().consumed)
    }

    @Test
    fun `redeeming token consumes it and replay is rejected`() {
        val token = "a".repeat(43)
        val record = ManagementToken(hash(token), "owner", Instant.now().plusSeconds(600))
        every { tokens.findForRedemption(hash(token)) } returns record
        every { subscriptions.existsByUserId("owner") } returns true
        assertEquals("owner", service.redeem(token))
        assertTrue(record.consumed)
        assertEquals(401, assertFailsWith<ResponseStatusException> { service.redeem(token) }.statusCode.value())
    }

    @Test
    fun `expired tokens cannot establish a session`() {
        val token = "b".repeat(43)
        every { tokens.findForRedemption(hash(token)) } returns ManagementToken(hash(token), "owner", Instant.now().minusSeconds(1))
        assertEquals(401, assertFailsWith<ResponseStatusException> { service.redeem(token) }.statusCode.value())
    }

    @Test
    fun `tokens for deleted subscriptions are rejected`() {
        val token = "c".repeat(43)
        every { tokens.findForRedemption(hash(token)) } returns ManagementToken(hash(token), "owner", Instant.now().plusSeconds(600))
        every { subscriptions.existsByUserId("owner") } returns false
        assertEquals(401, assertFailsWith<ResponseStatusException> { service.redeem(token) }.statusCode.value())
    }

    @Test
    fun `malformed and unknown tokens are rejected`() {
        assertEquals(401, assertFailsWith<ResponseStatusException> { service.redeem("bad-token") }.statusCode.value())
        every { tokens.findForRedemption(any()) } returns null
        assertEquals(401, assertFailsWith<ResponseStatusException> { service.redeem("d".repeat(43)) }.statusCode.value())
    }

    private fun hash(token: String) = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray()).joinToString("") { "%02x".format(it) }
}
