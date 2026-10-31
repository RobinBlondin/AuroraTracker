package com.example.auroratracker.service

import com.example.auroratracker.dto.ManageSubscriptionPatchDto
import com.example.auroratracker.entity.Subscription
import com.example.auroratracker.enums.NotificationLevel
import com.example.auroratracker.mapper.SubscriptionMapper
import com.example.auroratracker.repository.EmailVerificationRepository
import com.example.auroratracker.repository.SubscriptionRepository
import io.mockk.*
import org.junit.jupiter.api.Test
import org.springframework.web.server.ResponseStatusException
import java.util.Optional
import kotlin.test.*

class SubscriptionManagementServiceTest {
    private val repository = mockk<SubscriptionRepository>()
    private val verifications = mockk<EmailVerificationRepository>()
    private val service = SubscriptionService(repository, mockk(), mockk<SubscriptionMapper>(), EmailVerificationService(verifications))

    @Test
    fun `invalid coordinates are rejected before saving`() {
        for (patch in listOf(ManageSubscriptionPatchDto(lat = 91.0), ManageSubscriptionPatchDto(lon = -181.0), ManageSubscriptionPatchDto(lat = Double.NaN))) {
            val error = assertFailsWith<ResponseStatusException> { service.updateManagedSubscription("user", patch) }
            assertEquals(400, error.statusCode.value())
        }
        verify { repository wasNot Called }
    }

    @Test
    fun `partial updates retain delivery credentials and persist preferences`() {
        val sub = Subscription(email = "test@example.com", userId = "user", endpoint = "push-endpoint", lat = 60.0, lon = 18.0)
        every { repository.getByUserId("user") } returns Optional.of(sub)
        every { repository.save(sub) } returns sub
        val result = service.updateManagedSubscription("user", ManageSubscriptionPatchDto(notificationLevel = NotificationLevel.CLEAR, enabled = true))
        assertEquals(60.0, result.lat)
        assertEquals(NotificationLevel.CLEAR, result.notificationLevel)
        assertTrue(result.enabled)
        assertEquals("push-endpoint", sub.endpoint)
        verify(exactly = 1) { repository.save(sub) }
    }

    @Test
    fun `deletion clears verification references before deleting subscription`() {
        val sub = Subscription(email = null, userId = "user")
        every { repository.getByUserId("user") } returns Optional.of(sub)
        every { verifications.deleteBySubscription_UserId("user") } just Runs
        every { repository.delete(sub) } just Runs
        service.deleteManagedSubscription("user")
        verifyOrder { verifications.deleteBySubscription_UserId("user"); repository.delete(sub) }
    }

    @Test
    fun `missing subscription returns not found`() {
        every { repository.getByUserId("user") } returns Optional.empty()
        assertEquals(404, assertFailsWith<ResponseStatusException> { service.getManagedSubscription("user") }.statusCode.value())
    }
}
