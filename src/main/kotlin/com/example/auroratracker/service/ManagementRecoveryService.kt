package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.entity.ManagementToken
import com.example.auroratracker.repository.ManagementTokenRepository
import com.example.auroratracker.repository.SubscriptionRepository
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.server.ResponseStatusException
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.LocalDateTime
import java.util.Base64

@Service
class ManagementRecoveryService(
    private val subscriptions: SubscriptionRepository,
    private val tokens: ManagementTokenRepository,
    private val email: EmailService,
    private val webPush: WebPushService,
    private val firebase: FirebaseService,
    private val env: EnvConfig
) {
    private val random = SecureRandom()
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional
    fun requestEmail(address: String) {
        val subscription = subscriptions.findFirstByEmailIgnoreCase(address.trim()) ?: return
        issue(subscription.userId) { url -> email.sendManagementEmail(subscription.email!!, url) }
    }

    @Transactional
    fun requestPush(endpoint: String?, firebaseToken: String?) {
        val subscription = when {
            endpoint != null -> subscriptions.findFirstByEndpoint(endpoint)
            firebaseToken != null -> subscriptions.findFirstByFirebaseToken(firebaseToken)
            else -> null
        } ?: return
        issue(subscription.userId) { url ->
            if (subscription.firebaseToken != null) {
                firebase.sendManagementNotification(subscription.firebaseToken!!, url)
            } else {
                webPush.sendManagementNotification(subscription.endpoint!!, subscription.p256dh!!, subscription.auth!!, url)
            }
        }
    }

    private fun issue(userId: String, deliver: (String) -> Unit) {

        subscriptions.findForManagementIssuance(userId) ?: return
        val now = LocalDateTime.now()
        if (tokens.countByUserIdAndCreatedAtAfter(userId, now.minusMinutes(1)) > 0 ||
            tokens.countByUserIdAndCreatedAtAfter(userId, now.minusHours(1)) >= 5) return
        val bytes = ByteArray(32).also(random::nextBytes)
        val token = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
        val record = tokens.save(ManagementToken(hash(token), userId, Instant.now().plusSeconds(600)))
        val url = URI(env.resend.verificationUrl).resolve("/manage-alerts").toASCIIString() + "#token=" + token
        try {
            deliver(url)
        } catch (_: Exception) {
            record.consumed = true
            tokens.save(record)
            log.warn("Unable to deliver a subscription management link")
        }
    }

    @Transactional
    fun redeem(token: String): String {
        if (!token.matches(Regex("[A-Za-z0-9_-]{43}"))) invalidLink()
        val record = tokens.findForRedemption(hash(token)) ?: invalidLink()
        if (record.consumed || !Instant.now().isBefore(record.expiresAt) ||
            !subscriptions.existsByUserId(record.userId)) invalidLink()
        record.consumed = true
        tokens.save(record)
        return record.userId
    }

    @Scheduled(fixedDelay = 3_600_000)
    @Transactional
    fun removeOldTokens() {
        tokens.deleteByExpiresAtBefore(Instant.now().minusSeconds(86400))
    }

    private fun invalidLink(): Nothing = throw ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired management link")
    private fun hash(token: String) = MessageDigest.getInstance("SHA-256")
        .digest(token.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
