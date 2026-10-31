package com.example.auroratracker.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import java.time.Instant

@Entity
class ManagementToken(
    @Column(nullable = false, unique = true, length = 64)
    var tokenHash: String,
    @Column(nullable = false)
    var userId: String,
    @Column(nullable = false)
    var expiresAt: Instant,
    var consumed: Boolean = false
) : BaseEntity()
