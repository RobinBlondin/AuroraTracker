package com.example.auroratracker.controller

import com.example.auroratracker.service.SubscriptionValidation
import com.example.auroratracker.service.SubscriptionRequestGuard
import com.example.auroratracker.service.ManagementRecoveryService
import com.example.auroratracker.service.ManagementSessionService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.CacheControl
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/subscriptions/manage")
class ManagementRecoveryController(
    private val recovery: ManagementRecoveryService,
    private val sessions: ManagementSessionService,
    private val requests: SubscriptionRequestGuard
) {
    data class EmailRequest(val email: String)
    data class PushRequest(val endpoint: String? = null, val firebaseToken: String? = null)
    data class VerifyRequest(val token: String)

    @PostMapping("/email")
    fun email(request: HttpServletRequest, @RequestBody body: EmailRequest): ResponseEntity<Void> {
        requests.check(request)
        recovery.requestEmail(SubscriptionValidation.email(body.email))
        return accepted()
    }

    @PostMapping("/challenge")
    fun challenge(request: HttpServletRequest, @RequestBody body: PushRequest): ResponseEntity<Void> {
        requests.check(request)
        if ((body.endpoint.isNullOrBlank() == body.firebaseToken.isNullOrBlank()) ||
            (body.endpoint?.length ?: 0) > 4096 || (body.firebaseToken?.length ?: 0) > 4096) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "Provide one push identifier")
        }
        recovery.requestPush(body.endpoint?.takeIf { it.isNotBlank() }, body.firebaseToken?.takeIf { it.isNotBlank() })
        return accepted()
    }

    @PostMapping("/verify")
    fun verify(request: HttpServletRequest, @RequestBody body: VerifyRequest): ResponseEntity<Void> {
        requests.check(request)
        val userId = recovery.redeem(body.token)
        sessions.establish(request, userId)
        return ResponseEntity.noContent().cacheControl(CacheControl.noStore()).build()
    }

    private fun accepted(): ResponseEntity<Void> = ResponseEntity.accepted().cacheControl(CacheControl.noStore()).build()

}
