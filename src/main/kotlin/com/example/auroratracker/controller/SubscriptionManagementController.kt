package com.example.auroratracker.controller

import com.example.auroratracker.dto.ManageSubscriptionPatchDto
import com.example.auroratracker.service.ManagementSessionService
import com.example.auroratracker.service.SubscriptionService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/subscriptions/manage")
class SubscriptionManagementController(
    private val subscriptions: SubscriptionService,
    private val sessions: ManagementSessionService
) {
    @GetMapping
    fun get(request: HttpServletRequest): ResponseEntity<*> {
        val userId = sessions.requireUser(request)
        val subscription = subscriptions.getManagedSubscription(userId)
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore()).body(
            mapOf(
                "lat" to subscription.lat,
                "lon" to subscription.lon,
                "notificationLevel" to subscription.notificationLevel,
                "enabled" to subscription.enabled,
                "csrfToken" to request.getSession(false).getAttribute("managementCsrf")
            )
        )
    }

    @PatchMapping
    fun patch(request: HttpServletRequest, @RequestBody patch: ManageSubscriptionPatchDto): ResponseEntity<*> {
        val userId = sessions.requireUser(request, mutation = true)
        return ResponseEntity.ok().cacheControl(org.springframework.http.CacheControl.noStore())
            .body(subscriptions.updateManagedSubscription(userId, patch))
    }

    @DeleteMapping
    fun delete(request: HttpServletRequest): ResponseEntity<Void> {
        subscriptions.deleteManagedSubscription(sessions.requireUser(request, mutation = true))
        request.getSession(false).invalidate()
        return ResponseEntity.noContent().build()
    }
}
