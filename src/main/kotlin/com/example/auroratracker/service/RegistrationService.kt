package com.example.auroratracker.service

import com.example.auroratracker.dto.RegistrationDto
import com.example.auroratracker.dto.SubscriptionDto
import com.example.auroratracker.repository.SubscriptionRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.util.Base64
import java.util.UUID

@Service
class RegistrationService(
    private val subscriptions: SubscriptionService,
    private val repository: SubscriptionRepository,
    private val emailService: EmailService
) {
    data class Result(val userId: String?, val verificationRequired: Boolean)

    fun register(input: RegistrationDto): Result {
        if (input.lat == null || input.lon == null) SubscriptionValidation.invalid("Location is required")
        SubscriptionValidation.location(input.lat, input.lon)
        val email = input.email?.let(SubscriptionValidation::email)
        val hasWebPush = input.endpoint != null || input.p256dh != null || input.auth != null
        val hasFirebase = input.firebaseToken != null
        if (hasWebPush && hasFirebase) SubscriptionValidation.invalid("Choose one push delivery method")
        if (email == null && !hasWebPush && !hasFirebase) SubscriptionValidation.invalid("Choose email or push alerts")
        if (hasWebPush) validateWebPush(input)
        input.firebaseToken?.let {
            if (!it.matches(Regex("[A-Za-z0-9:_-]{1,4096}"))) SubscriptionValidation.invalid("Invalid Firebase token")
        }

        if (email != null && repository.findFirstByEmailIgnoreCase(email) != null) return Result(null, true)
        val duplicatePush = input.endpoint?.let { repository.findFirstByEndpoint(it) != null } == true ||
            input.firebaseToken?.let { repository.findFirstByFirebaseToken(it) != null } == true
        if (duplicatePush) {
            if (email != null) return Result(null, true)
            throw ResponseStatusException(HttpStatus.CONFLICT, "This browser is already registered. Use Manage alerts.")
        }

        val dto = SubscriptionDto(
            userId = UUID.randomUUID().toString(), email = email,
            endpoint = input.endpoint, p256dh = input.p256dh, auth = input.auth,
            firebaseToken = input.firebaseToken, lat = input.lat, lon = input.lon,
            notificationLevel = input.notificationLevel, enabled = email == null
        )
        if (email != null) {
            val verification = subscriptions.createSubscription(dto)
            emailService.sendVerificationEmail(email, verification.token!!)
            return Result(null, true)
        }
        return Result(subscriptions.saveSub(dto).userId!!, false)
    }

    private fun validateWebPush(input: RegistrationDto) {
        val endpoint = input.endpoint ?: SubscriptionValidation.invalid("Missing push endpoint")
        if (endpoint.length > 4096) SubscriptionValidation.invalid("Invalid push endpoint")
        val url = runCatching { URI(endpoint) }.getOrNull()
        val providers = setOf("fcm.googleapis.com", "updates.push.services.mozilla.com", "web.push.apple.com")

        if (url == null || url.scheme != "https" || url.host !in providers ||
            url.userInfo != null || url.port !in listOf(-1, 443) || url.fragment != null || url.path.isNullOrEmpty()) {
            SubscriptionValidation.invalid("Unsupported push endpoint")
        }
        val key = decodeKey(input.p256dh, 65)
        if (key[0] != 4.toByte()) SubscriptionValidation.invalid("Invalid push public key")
        decodeKey(input.auth, 16)
    }

    private fun decodeKey(value: String?, length: Int): ByteArray {
        if (value == null || value.length > 128 || !value.matches(Regex("[A-Za-z0-9_-]+={0,2}"))) {
            SubscriptionValidation.invalid("Invalid push encryption keys")
        }
        val decoded = runCatching { Base64.getUrlDecoder().decode(value) }.getOrNull()
        if (decoded == null || decoded.size != length) SubscriptionValidation.invalid("Invalid push encryption keys")
        return decoded
    }
}
