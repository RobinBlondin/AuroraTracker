package com.example.auroratracker.service

import com.example.auroratracker.entity.EmailVerification
import com.example.auroratracker.repository.EmailVerificationRepository
import com.example.auroratracker.repository.SubscriptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class VerificationResult(
    val status: String,
    val userId: String? = null
)


@Service
class EmailVerificationService(
    val repo: EmailVerificationRepository,
    private val subscriptions: SubscriptionRepository
) {
    @Transactional
    fun confirmEmail(token: String): VerificationResult {
        val verification = repo.findByTokenForUpdate(token)
            ?: return VerificationResult("invalid")

        if (!LocalDateTime.now().isBefore(verification.expiresAt)) {
            return VerificationResult("expired")
        }

        val subscription = verification.subscription
            ?: return VerificationResult("invalid")

        if (subscription.enabled) {
            repo.delete(verification)
            return VerificationResult("invalid")
        }

        subscription.enabled = true
        subscriptions.save(subscription)
        repo.delete(verification)

        return VerificationResult(
            status = "success",
            userId = subscription.userId
        )
    }
    fun getVerificationById(id: Long) = repo.findById(id)
    fun getVerificationByToken(token: String) = repo.findByToken(token)
    fun existsByToken(token: String): Boolean = repo.existsByToken(token)
    fun save(verification: EmailVerification) {
        repo.save(verification)
    }
    @Transactional
    fun delete(token: String) = repo.deleteByToken(token)
    fun tokenHasExpired(verification: EmailVerification): Boolean = verification.expiresAt < LocalDateTime.now()



}