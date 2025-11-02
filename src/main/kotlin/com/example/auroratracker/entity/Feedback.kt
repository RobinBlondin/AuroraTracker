package com.example.auroratracker.entity

import com.example.auroratracker.enums.FeedbackState
import jakarta.persistence.Entity
import java.time.LocalDateTime
import java.util.UUID

@Entity
class Feedback(
      var state: FeedbackState = FeedbackState.PENDING,
      var token: UUID = UUID.randomUUID(),
      var expires: LocalDateTime = LocalDateTime.now().plusHours(12),
      ):BaseEntity()