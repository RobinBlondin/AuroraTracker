package com.example.auroratracker.entity

import jakarta.persistence.*
import org.springframework.data.annotation.Id
import java.time.LocalDateTime
import java.util.*

@Entity
class EmailVerification(
    @ManyToOne(fetch = FetchType.LAZY)
    var subscription: Subscription? = null,

    @Column(nullable = false, unique = true)
    var token: String? = "",

    @Column(nullable = false)
    var expiresAt: LocalDateTime = LocalDateTime.now().plusHours(24)
): BaseEntity()