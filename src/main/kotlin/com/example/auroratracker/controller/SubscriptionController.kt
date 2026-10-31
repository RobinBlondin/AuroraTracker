package com.example.auroratracker.controller

import com.example.auroratracker.dto.RegistrationDto
import com.example.auroratracker.service.ManagementSessionService
import com.example.auroratracker.service.RegistrationService
import com.example.auroratracker.service.SubscriptionRequestGuard
import com.example.auroratracker.service.SubscriptionRequestRateLimiter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping(value = ["/api/subscriptions"])
class SubscriptionController(
      private val requests: SubscriptionRequestGuard,
      private val registration: RegistrationService,
      private val managementSessions: ManagementSessionService
) {
      @PostMapping("subscribe")
      fun subscribe(
            request: HttpServletRequest,
            @RequestBody dto: RegistrationDto
      ): ResponseEntity<Map<String, String>> {
            requests.check(request, SubscriptionRequestRateLimiter.Purpose.SIGNUP)
            val result = registration.register(dto)
            if (result.verificationRequired) {
                  return ResponseEntity.accepted().cacheControl(CacheControl.noStore())
                        .body(mapOf("status" to "verification_required"))
            }
            managementSessions.establish(request, result.userId!!)
            return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
                  .body(mapOf("status" to "active"))
      }
}