package com.example.auroratracker.service

import com.example.auroratracker.entity.Notification
import com.example.auroratracker.repository.NotificationRepository
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class NotificationService(
      private val notificationRepository: NotificationRepository,
) {
      fun add(notification: Notification): Notification {
            return notificationRepository.save(notification)
      }

      fun countTotal(): Int = notificationRepository.findAll().count()

      fun countToday(): Int = notificationRepository.findAll().count {
            it.createdAt.toString().split("T")[0] == LocalDateTime.now().toString().split("T")[0]
      }

}