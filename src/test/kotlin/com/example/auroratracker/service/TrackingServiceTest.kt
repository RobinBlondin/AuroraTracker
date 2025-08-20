package com.example.auroratracker.service

import com.example.auroratracker.dto.AuroraPointsDto
import com.example.auroratracker.dto.UserDto
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TrackingServiceTest {
      val jsonService = mockk<JsonService>()
      val userService = mockk<UserService>()
      val emailService = mockk<EmailService>()
      val trackingService = spyk(TrackingService(jsonService, userService, emailService))

      val userInRange = UserDto(
            id = 1L,
            name = "Test User",
            email = "test@example.com",
            phoneNumber = "+123456789",
            lon = 17.474,
            lat = 58.947,
            lastNotificationTime = null
      )

      @Test
      fun checkAuroraForUsers_userNearAuroraPoint_sendsEmailAndUpdatesTimestamp() = runBlocking {

            every { userService.getAllUsers() } returns listOf(userInRange)
            every { userService.isAfterSunsetAndClearSky(userInRange) } returns true
            every { userService.hasUserReceivedNotificationRecently(userInRange) } returns false
            every { userService.updateLastNotificationTime(any()) } answers { firstArg() }

            val auroraDto = AuroraPointsDto().apply {
                  coordinates = listOf(listOf(10, 60, 60))
            }

            every { trackingService.getAuroraPoints() } returns auroraDto.coordinates!!
            every { trackingService.getKpIndex() } returns 5
            coEvery { emailService.sendEmailAsync("test@example.com") } returns CompletableDeferred(true)

            trackingService.checkAuroraForUsers()

            coVerify(exactly = 1) { emailService.sendEmailAsync("test@example.com") }
            verify(exactly = 1) { userService.updateLastNotificationTime(userInRange) }
            assertNotNull(
                  userInRange.lastNotificationTime,
                  "User lastNotificationTime should be set after notification"
            )
      }

      @Test
      fun checkAuroraForUsers_userTooFarFromAuroraPoint_doesNotSendEmail() = runBlocking {

            val userNotInRange = UserDto(
                  id = 1L,
                  name = "Test User",
                  email = "test@example.com",
                  phoneNumber = "+123456789",
                  lon = 17.474,
                  lat = 50.947,
                  lastNotificationTime = null
            )

            every { userService.getAllUsers() } returns listOf(userNotInRange)
            every { userService.isAfterSunsetAndClearSky(userNotInRange) } returns true
            every { userService.hasUserReceivedNotificationRecently(userNotInRange) } returns false
            every { userService.updateLastNotificationTime(any()) } answers { firstArg() }

            val auroraDto = AuroraPointsDto().apply {
                  coordinates = listOf(listOf(20, 60, 60))
            }

            every { trackingService.getAuroraPoints() } returns auroraDto.coordinates!!
            every { trackingService.getKpIndex() } returns 5
            coEvery { emailService.sendEmailAsync("test@example.com") } returns CompletableDeferred(true)

            trackingService.checkAuroraForUsers()

            coVerify(exactly = 0) { emailService.sendEmailAsync("test@example.com") }
            verify(exactly = 0) { userService.updateLastNotificationTime(userNotInRange) }
            assertNull(
                  userNotInRange.lastNotificationTime,
                  "User lastNotificationTime should not be set if no notification was sent"
            )
      }

      @Test
      fun checkAuroraForUsers_userBeforeSunsetOrCloudy_doesNotSendEmail() = runBlocking {
            every { userService.getAllUsers() } returns listOf(userInRange)
            every { userService.isAfterSunsetAndClearSky(userInRange) } returns false
            every { userService.hasUserReceivedNotificationRecently(userInRange) } returns false
            every { userService.updateLastNotificationTime(any()) } answers { firstArg() }

            val auroraDto = AuroraPointsDto().apply {
                  coordinates = listOf(listOf(10, 60, 60))
            }

            every { trackingService.getAuroraPoints() } returns auroraDto.coordinates!!
            every { trackingService.getKpIndex() } returns 5
            coEvery { emailService.sendEmailAsync("test@example.com") } returns CompletableDeferred(true)

            trackingService.checkAuroraForUsers()

            coVerify(exactly = 0) { emailService.sendEmailAsync("test@example.com") }
            verify(exactly = 0) { userService.updateLastNotificationTime(userInRange) }
            assertNull(
                  userInRange.lastNotificationTime,
                  "User lastNotificationTime should not be set if no notification was sent"
            )
      }

      @Test
      fun checkAuroraForUsers_userRecentlyNotified_doesNotSendEmail() = runBlocking {
            every { userService.getAllUsers() } returns listOf(userInRange)
            every { userService.isAfterSunsetAndClearSky(userInRange) } returns true
            every { userService.hasUserReceivedNotificationRecently(userInRange) } returns true
            every { userService.updateLastNotificationTime(any()) } answers { firstArg() }

            val auroraDto = AuroraPointsDto().apply {
                  coordinates = listOf(listOf(10, 60, 60))
            }

            every { trackingService.getAuroraPoints() } returns auroraDto.coordinates!!
            every { trackingService.getKpIndex() } returns 5
            coEvery { emailService.sendEmailAsync("test@example.com") } returns CompletableDeferred(true)

            trackingService.checkAuroraForUsers()

            coVerify(exactly = 0) { emailService.sendEmailAsync("test@example.com") }
            verify(exactly = 0) { userService.updateLastNotificationTime(userInRange) }
            assertNull(
                  userInRange.lastNotificationTime,
                  "User lastNotificationTime should not be set if no notification was sent"
            )
      }

      @Test
      fun checkAuroraForUsers_kpIndexTooLow_doesNotSendEmail() = runBlocking {
            every { userService.getAllUsers() } returns listOf(userInRange)
            every { userService.isAfterSunsetAndClearSky(userInRange) } returns true
            every { userService.hasUserReceivedNotificationRecently(userInRange) } returns false
            every { userService.updateLastNotificationTime(any()) } answers { firstArg() }

            val auroraDto = AuroraPointsDto().apply {
                  coordinates = listOf(listOf(10, 60, 60))
            }

            every { trackingService.getAuroraPoints() } returns auroraDto.coordinates!!
            every { trackingService.getKpIndex() } returns 1
            coEvery { emailService.sendEmailAsync("test@example.com") } returns CompletableDeferred(true)

            trackingService.checkAuroraForUsers()

            coVerify(exactly = 0) { emailService.sendEmailAsync("test@example.com") }
            verify(exactly = 0) { userService.updateLastNotificationTime(userInRange) }
            assertNull(
                  userInRange.lastNotificationTime,
                  "User lastNotificationTime should not be set if no notification was sent"
            )
      }

      @Test
      fun checkAuroraForUsers_auroraPointProbabilityTooLow_doesNotSendEmail() = runBlocking {
            every { userService.getAllUsers() } returns listOf(userInRange)
            every { userService.isAfterSunsetAndClearSky(userInRange) } returns true
            every { userService.hasUserReceivedNotificationRecently(userInRange) } returns false
            every { userService.updateLastNotificationTime(any()) } answers { firstArg() }

            val auroraDto = AuroraPointsDto().apply {
                  coordinates = listOf(listOf(10, 60, 1))
            }

            every { trackingService.getAuroraPoints() } returns auroraDto.coordinates!!
            every { trackingService.getKpIndex() } returns 5
            coEvery { emailService.sendEmailAsync("test@example.com") } returns CompletableDeferred(true)

            trackingService.checkAuroraForUsers()

            coVerify(exactly = 0) { emailService.sendEmailAsync("test@example.com") }
            verify(exactly = 0) { userService.updateLastNotificationTime(userInRange) }
            assertNull(
                  userInRange.lastNotificationTime,
                  "User lastNotificationTime should not be set if no notification was sent"
            )
      }
}