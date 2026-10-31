package com.example.auroratracker.controller

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.dto.RegistrationDto
import com.example.auroratracker.service.*
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import kotlin.test.*

class RegistrationControllerTest {
    private val registration = mockk<RegistrationService>()
    private val env = mockk<EnvConfig>()
    private lateinit var mvc: MockMvc
    private lateinit var limiter: SubscriptionRequestRateLimiter
    private val body = """{"lat":60,"lon":18,"notificationLevel":"CLEAR","firebaseToken":"fcm-token"}"""

    @BeforeEach
    fun setup() {
        every { env.resend } returns EnvConfig.ResendConfig("unused", "http://localhost/verification/")
        limiter = SubscriptionRequestRateLimiter()
        mvc = MockMvcBuilders.standaloneSetup(SubscriptionController(
            mockk(), env, SubscriptionRequestGuard(limiter, env), registration, ManagementSessionService()
        )).build()
    }

    private fun request(payload: String = body) = post("/api/subscriptions/subscribe")
        .header("X-Subscription-Request", "1").contentType(MediaType.APPLICATION_JSON).content(payload)

    @Test
    fun `signup does not need an API secret and returns no credentials`() {
        every { registration.register(any()) } returns RegistrationService.Result("server-user", false)
        val result = mvc.perform(request()).andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("active"))
            .andExpect(jsonPath("$.firebaseToken").doesNotExist()).andReturn()
        assertEquals("server-user", result.request.getSession(false)?.getAttribute("managementUserId"))
    }

    @Test
    fun `unverified email registration has no management session`() {
        every { registration.register(any()) } returns RegistrationService.Result(null, true)
        val result = mvc.perform(request()).andExpect(status().isAccepted)
            .andExpect(jsonPath("$.status").value("verification_required")).andReturn()
        assertNull(result.request.getSession(false))
    }

    @Test
    fun `signup cannot accept client database IDs or internal state`() {
        val supplied = slot<RegistrationDto>()
        every { registration.register(capture(supplied)) } returns RegistrationService.Result("fresh-user", false)
        mvc.perform(request(body.dropLast(1) + """, "id":42,"userId":"victim","enabled":true,"lastNotificationTime":"2026-01-01T00:00:00Z"}"""))
            .andExpect(status().isCreated)
        assertFalse(supplied.captured.javaClass.declaredFields.any { it.name in setOf("id", "userId", "enabled", "lastNotificationTime") })
    }

    @Test
    fun `signup rate limit cannot exhaust recovery allowance`() {
        every { registration.register(any()) } returns RegistrationService.Result(null, true)
        repeat(5) { mvc.perform(request()).andExpect(status().isAccepted) }
        mvc.perform(request()).andExpect(status().isTooManyRequests)
        repeat(10) { limiter.check("127.0.0.1", SubscriptionRequestRateLimiter.Purpose.RECOVERY) }
        verify(exactly = 5) { registration.register(any()) }
    }

    @Test
    fun `cross origin registration and missing request header are forbidden`() {
        mvc.perform(request().header("Origin", "https://other.example")).andExpect(status().isForbidden)
        mvc.perform(post("/api/subscriptions/subscribe").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isForbidden)
        verify { registration wasNot Called }
    }

    @Test
    fun `public keys response does not include the server API secret`() {
        every { env.vapid } returns EnvConfig.VapidConfig("subject", "public-vapid", "private-vapid")
        every { env.firebase } returns EnvConfig.FirebaseConfig("private-service-account", "firebase-vapid", "api", "auth", "project", "storage", "sender", "app", "measurement")
        every { env.carto } returns EnvConfig.CartoConfig("carto-public")
        val keys = ConfigController(env).keys()
        assertFalse(keys.contains("api-secret"))
        assertFalse(keys.contains("secretKey"))
        assertFalse(keys.contains("private-vapid"))
        assertTrue(keys.contains("public-vapid"))
    }
}
