package com.example.auroratracker.repository

import com.example.auroratracker.entity.Subscription
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.repository.query.Param
import java.util.Optional

interface SubscriptionRepository: JpaRepository<Subscription, Long> {
      @Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
      @Query("select s from Subscription s where s.userId = :userId")
      fun findForManagementIssuance(@Param("userId") userId: String): Subscription?

      fun findFirstByEmailIgnoreCase(email: String): Subscription?
      fun findFirstByEndpoint(endpoint: String): Subscription?
      fun findFirstByFirebaseToken(firebaseToken: String): Subscription?
      fun getByUserId(id: String): Optional<Subscription>
      fun existsByUserId(userId: String): Boolean
      fun deleteByUserId(id: String)
}