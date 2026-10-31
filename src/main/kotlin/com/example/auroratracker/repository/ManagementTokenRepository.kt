package com.example.auroratracker.repository

import com.example.auroratracker.entity.ManagementToken
import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.time.LocalDateTime

interface ManagementTokenRepository : JpaRepository<ManagementToken, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ManagementToken t where t.tokenHash = :hash")
    fun findForRedemption(@Param("hash") hash: String): ManagementToken?

    fun countByUserIdAndCreatedAtAfter(userId: String, after: LocalDateTime): Long
    fun deleteByExpiresAtBefore(before: Instant)
}
