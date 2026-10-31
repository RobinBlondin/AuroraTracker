package com.example.auroratracker.service

import com.example.auroratracker.dto.RegistrationDto
import com.example.auroratracker.dto.SubscriptionDto
import com.example.auroratracker.entity.EmailVerification
import com.example.auroratracker.entity.Subscription
import com.example.auroratracker.enums.NotificationLevel
import com.example.auroratracker.repository.SubscriptionRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.util.Base64
import java.util.UUID
import kotlin.test.*

class RegistrationServiceTest {
    private val subscriptions = mockk<SubscriptionService>()
    private val repository = mockk<SubscriptionRepository>()
    private val email = mockk<EmailService>()
    private val service = RegistrationService(subscriptions, repository, email)
    private val input = RegistrationDto(email = "USER@example.com", lat = 60.0, lon = 18.0, notificationLevel = NotificationLevel.CLEAR)

    @BeforeEach
    fun setup() {
        every { repository.findFirstByEmailIgnoreCase(any()) } returns null
        every { repository.findFirstByEndpoint(any()) } returns null
        every { repository.findFirstByFirebaseToken(any()) } returns null
    }

    @Test
    fun `email signup normalizes address assigns identity and waits for verification`() {
        val saved = slot<SubscriptionDto>()
        every { subscriptions.createSubscription(capture(saved)) } returns EmailVerification(token = "verification")
        every { email.sendVerificationEmail("user@example.com", "verification") } just Runs
        val result = service.register(input)
        assertTrue(result.verificationRequired)
        assertNull(result.userId)
        assertNull(saved.captured.id)
        assertNotNull(UUID.fromString(saved.captured.userId))
        assertEquals("user@example.com", saved.captured.email)
        assertEquals(false, saved.captured.enabled)
        verify(exactly = 1) { email.sendVerificationEmail("user@example.com", "verification") }
    }

    @Test
    fun `push signup saves a fresh subscription without email`() {
        val saved = slot<SubscriptionDto>()
        every { subscriptions.saveSub(capture(saved)) } answers { firstArg() }
        val result = service.register(input.copy(email = null, firebaseToken = "fcm-token"))
        assertFalse(result.verificationRequired)
        assertEquals(saved.captured.userId, result.userId)
        assertNull(saved.captured.id)
        assertEquals(true, saved.captured.enabled)
        verify { email wasNot Called }
    }

    @Test
    fun `duplicate email returns generic verification response without sending more mail`() {
        every { repository.findFirstByEmailIgnoreCase("user@example.com") } returns Subscription(email = "user@example.com", userId = "existing")
        val result = service.register(input)
        assertTrue(result.verificationRequired)
        assertNull(result.userId)
        verify { email wasNot Called; subscriptions wasNot Called }
    }

    @Test
    fun `duplicate push identifier does not create another subscription or session`() {
        every { repository.findFirstByFirebaseToken("fcm-token") } returns Subscription(email = null, userId = "existing")
        val error = assertFailsWith<ResponseStatusException> { service.register(input.copy(email = null, firebaseToken = "fcm-token")) }
        assertEquals(409, error.statusCode.value())
        verify { subscriptions wasNot Called }
    }

    @Test
    fun `malformed signup data never reaches storage or email`() {
        val invalid = listOf(
            input.copy(lat = null), input.copy(lon = null), input.copy(lat = 91.0),
            input.copy(lon = Double.NaN), input.copy(email = "broken"), input.copy(email = ""),
            input.copy(email = null), input.copy(endpoint = "https://web.push.apple.com/path"),
            input.copy(firebaseToken = " "), input.copy(endpoint = "endpoint", firebaseToken = "fcm-token")
        )
        for (value in invalid) {
            assertEquals(400, assertFailsWith<ResponseStatusException> { service.register(value) }.statusCode.value())
        }
        verify { subscriptions wasNot Called; email wasNot Called }
    }

    @Test
    fun `valid web push keys and supported provider are accepted`() {
        every { subscriptions.saveSub(any()) } answers { firstArg() }
        val key = ByteArray(65).also { it[0] = 4 }
        val encode = Base64.getUrlEncoder().withoutPadding()
        val push = input.copy(email = null, endpoint = "https://updates.push.services.mozilla.com/wpush/v2/token",
            p256dh = encode.encodeToString(key), auth = encode.encodeToString(ByteArray(16)))
        assertFalse(service.register(push).verificationRequired)
    }

    @Test
    fun `web push endpoints cannot target arbitrary servers`() {
        val encode = Base64.getUrlEncoder().withoutPadding()
        val push = input.copy(p256dh = encode.encodeToString(ByteArray(65).also { it[0] = 4 }), auth = encode.encodeToString(ByteArray(16)))
        for (endpoint in listOf("http://127.0.0.1/private", "https://internal.example/private", "https://web.push.apple.com.evil.example/token", "https://web.push.apple.com:8443/token")) {
            assertEquals(400, assertFailsWith<ResponseStatusException> { service.register(push.copy(endpoint = endpoint)) }.statusCode.value())
        }
        verify { subscriptions wasNot Called }
    }
}
