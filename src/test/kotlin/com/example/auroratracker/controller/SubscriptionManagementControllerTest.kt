package com.example.auroratracker.controller

import com.example.auroratracker.dto.ManageSubscriptionDto
import com.example.auroratracker.enums.NotificationLevel
import com.example.auroratracker.service.ManagementSessionService
import com.example.auroratracker.service.SubscriptionService
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.*
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import java.time.Instant
import kotlin.test.assertTrue

class SubscriptionManagementControllerTest {
    private val subscriptions = mockk<SubscriptionService>()
    private val sessions = ManagementSessionService()
    private lateinit var mvc: MockMvc
    private lateinit var session: MockHttpSession
    private lateinit var csrf: String
    private val preferences = ManageSubscriptionDto(59.3, 18.0, NotificationLevel.CLEAR, true)

    @BeforeEach
    fun setup() {
        clearMocks(subscriptions)
        mvc = MockMvcBuilders.standaloneSetup(SubscriptionManagementController(subscriptions, sessions)).build()
        val request = MockHttpServletRequest()
        sessions.establish(request, "current-user")
        session = request.session as MockHttpSession
        csrf = session.getAttribute("managementCsrf") as String
    }

    @Test
    fun `requests without a management session are unauthorized`() {
        mvc.perform(get("/api/subscriptions/manage")).andExpect(status().isUnauthorized)
        mvc.perform(patch("/api/subscriptions/manage").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isUnauthorized)
        mvc.perform(delete("/api/subscriptions/manage")).andExpect(status().isUnauthorized)
        verify { subscriptions wasNot Called }
    }

    @Test
    fun `get loads only the session subscription without caching`() {
        every { subscriptions.getManagedSubscription("current-user") } returns preferences
        mvc.perform(get("/api/subscriptions/manage").session(session))
            .andExpect(status().isOk).andExpect(jsonPath("$.notificationLevel").value("CLEAR"))
            .andExpect(jsonPath("$.csrfToken").value(csrf)).andExpect(header().string("Cache-Control", "no-store"))
    }

    @Test
    fun `expired sessions cannot access subscriptions`() {
        session.setAttribute("managementExpiresAt", Instant.now().minusSeconds(1))
        mvc.perform(get("/api/subscriptions/manage").session(session)).andExpect(status().isUnauthorized)
        assertTrue(session.isInvalid)
    }

    @Test
    fun `mutations require a csrf token`() {
        mvc.perform(patch("/api/subscriptions/manage").session(session)
            .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isForbidden)
        mvc.perform(delete("/api/subscriptions/manage").session(session)).andExpect(status().isForbidden)
        verify { subscriptions wasNot Called }
    }

    @Test
    fun `patch supports partial updates for the session subscription`() {
        every { subscriptions.updateManagedSubscription("current-user", match { it.enabled == false && it.lat == null }) } returns preferences.copy(enabled = false)
        mvc.perform(patch("/api/subscriptions/manage").session(session).header("X-CSRF-Token", csrf)
            .contentType(MediaType.APPLICATION_JSON).content("{\"enabled\":false}"))
            .andExpect(status().isOk).andExpect(jsonPath("$.enabled").value(false))
    }

    @Test
    fun `unknown notification levels are rejected`() {
        mvc.perform(patch("/api/subscriptions/manage").session(session).header("X-CSRF-Token", csrf)
            .contentType(MediaType.APPLICATION_JSON).content("{\"notificationLevel\":\"INVALID\"}"))
            .andExpect(status().isBadRequest)
    }

    @Test
    fun `delete removes subscription and invalidates the session`() {
        every { subscriptions.deleteManagedSubscription("current-user") } just Runs
        mvc.perform(delete("/api/subscriptions/manage").session(session).header("X-CSRF-Token", csrf))
            .andExpect(status().isNoContent)
        verify(exactly = 1) { subscriptions.deleteManagedSubscription("current-user") }
        assertTrue(session.isInvalid)
    }
}
