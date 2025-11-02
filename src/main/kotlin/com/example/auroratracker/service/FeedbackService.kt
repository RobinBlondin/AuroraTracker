package com.example.auroratracker.service

import com.example.auroratracker.entity.Feedback
import com.example.auroratracker.repository.FeedbackRepository
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class FeedbackService(
      private val feedbackRepository: FeedbackRepository,
) {
      fun getFeedback(token: UUID): Feedback? {
            return feedbackRepository.getFeedbackByToken(token)
      }

      fun saveFeedback(feedback: Feedback) {
            feedbackRepository.save(feedback)
      }
}