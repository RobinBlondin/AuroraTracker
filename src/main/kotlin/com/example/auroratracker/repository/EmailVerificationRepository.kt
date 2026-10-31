package com.example.auroratracker.repository

import com.example.auroratracker.entity.EmailVerification
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface EmailVerificationRepository: JpaRepository<EmailVerification, Long> {
    fun findByToken(hash: String): EmailVerification?
    fun deleteBySubscription_UserId(userId: String)
    fun existsByToken(token: String): Boolean
    fun deleteByToken(token: String)

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select v from EmailVerification v where v.token = :token")
    fun findByTokenForUpdate(@Param("token") token: String): EmailVerification?
}