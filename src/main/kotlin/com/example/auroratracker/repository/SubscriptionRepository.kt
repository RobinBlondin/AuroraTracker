package com.example.auroratracker.repository

import com.example.auroratracker.entity.Subscription
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional

interface SubscriptionRepository: JpaRepository<Subscription, Long> {
      fun getByUserId(id: String): Optional<Subscription>
      fun existsByUserId(userId: String): Boolean
      fun removeByUserId(id: String)
}