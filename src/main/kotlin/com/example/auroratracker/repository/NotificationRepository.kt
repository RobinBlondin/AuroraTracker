package com.example.auroratracker.repository

import com.example.auroratracker.entity.Notification
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository


interface NotificationRepository: JpaRepository<Notification, Long> {
}