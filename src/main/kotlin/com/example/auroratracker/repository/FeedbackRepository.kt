package com.example.auroratracker.repository

import com.example.auroratracker.entity.Feedback
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface FeedbackRepository: JpaRepository<Feedback, Long> {
      fun getFeedbackByToken(token: UUID): Feedback?
}