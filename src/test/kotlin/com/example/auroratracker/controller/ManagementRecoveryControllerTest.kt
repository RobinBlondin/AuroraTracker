package com.example.auroratracker.controller

import com.example.auroratracker.service.SubscriptionRequestRateLimiter
import com.example.auroratracker.service.ManagementRecoveryService
import com.example.auroratracker.service.ManagementSessionService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ManagementRecoveryControllerTest {
    private val recovery = mockk<ManagementRecoveryService>()
    private lateinit var mvc: MockMvc

    @BeforeEach
    fun setup() {
        clearMocks(recovery)
        val env = mockk<com.example.auroratracker.config.EnvConfig>()
        every { env.resend } returns com.example.auroratracker.config.EnvConfig.ResendConfig("unused", "http://localhost/verification/")
        mvc = MockMvcBuilders.standaloneSetup(ManagementRecoveryController(
            recovery, ManagementSessionService(), com.example.auroratracker.service.SubscriptionRequestGuard(SubscriptionRequestRateLimiter(), env)
        )).build()
    }

    private fun request(path: String, body: String) = post("/api/subscriptions/manage/$path")
        .header("X-Management-Request", "1").contentType(MediaType.APPLICATION_JSON).content(body)

    @Test
    fun `email requests return a generic empty response without authenticating`() {
        every { recovery.requestEmail(any()) } just Runs
        val result = mvc.perform(request("email", "{\"email\":\"person@example.com\"}"))
            .andExpect(status().isAccepted).andExpect(content().string(""))
            .andExpect(header().string("Cache-Control", "no-store")).andReturn()
        assertEquals(null, result.request.getSession(false))
    }

    @Test
    fun `cross site requests are rejected`() {
        mvc.perform(request("email", "{\"email\":\"person@example.com\"}").header("Origin", "https://other.example"))
            .andExpect(status().isForbidden)
        mvc.perform(request("verify", "{\"token\":\"token\"}").header("Sec-Fetch-Site", "cross-site"))
            .andExpect(status().isForbidden)
        verify { recovery wasNot Called }
    }

    @Test
    fun `same origin browser requests are accepted`() {
        every { recovery.requestEmail(any()) } just Runs
        mvc.perform(request("email", "{\"email\":\"person@example.com\"}").header("Origin", "http://localhost"))
            .andExpect(status().isAccepted)
    }

    @Test
    fun `missing custom header is forbidden`() {
        mvc.perform(post("/api/subscriptions/manage/verify").contentType(MediaType.APPLICATION_JSON)
            .content("{\"token\":\"token\"}")).andExpect(status().isForbidden)
        verify { recovery wasNot Called }
    }

    @Test
    fun `valid token rotates session and grants management access`() {
        every { recovery.redeem("token") } returns "owner"
        val previousSession = MockHttpSession()
        val result = mvc.perform(request("verify", "{\"token\":\"token\"}").session(previousSession))
            .andExpect(status().isNoContent).andReturn()
        assertTrue(previousSession.isInvalid)
        val session = result.request.getSession(false)
        assertNotNull(session)
        assertEquals("owner", session.getAttribute("managementUserId"))
        assertNotNull(session.getAttribute("managementCsrf"))
    }

    @Test
    fun `invalid token never creates a management session`() {
        every { recovery.redeem(any()) } throws ResponseStatusException(HttpStatus.UNAUTHORIZED)
        val result = mvc.perform(request("verify", "{\"token\":\"bad\"}"))
            .andExpect(status().isUnauthorized).andReturn()
        assertEquals(null, result.request.getSession(false))
    }

    @Test
    fun `requests are rate limited independently of account existence`() {
        every { recovery.requestEmail(any()) } just Runs
        repeat(10) {
            mvc.perform(request("email", "{\"email\":\"person@example.com\"}")).andExpect(status().isAccepted)
        }
        mvc.perform(request("email", "{\"email\":\"person@example.com\"}")).andExpect(status().isTooManyRequests)
        verify(exactly = 10) { recovery.requestEmail(any()) }
    }

    @Test
    fun `invalid addresses and ambiguous push identifiers are rejected`() {
        mvc.perform(request("email", "{\"email\":\"invalid\"}")).andExpect(status().isBadRequest)
        mvc.perform(request("challenge", "{}")).andExpect(status().isBadRequest)
        mvc.perform(request("challenge", "{\"endpoint\":\"one\",\"firebaseToken\":\"two\"}"))
            .andExpect(status().isBadRequest)
        verify { recovery wasNot Called }
    }

    @Test
    fun `endpoint and firebase challenges return no account details or session`() {
        every { recovery.requestPush(any(), any()) } just Runs
        for (body in listOf("{\"endpoint\":\"push-endpoint\"}", "{\"firebaseToken\":\"fcm-token\"}")) {
            val result = mvc.perform(request("challenge", body)).andExpect(status().isAccepted)
                .andExpect(content().string("")).andReturn()
            assertEquals(null, result.request.getSession(false))
        }
    }
}
