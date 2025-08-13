package com.example.auroratracker.entity

import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime

@MappedSuperclass
abstract class BaseEntity (
      @Id
      @GeneratedValue(strategy = GenerationType.AUTO)
      var id: Long? = null,
      var createdAt: LocalDateTime? = null,
      var updatedAt: LocalDateTime? = null
) {
      @PrePersist
      fun prePersist() {
            val now = LocalDateTime.now()
            if (createdAt == null) {
                  createdAt = now
            }
            updatedAt = now
      }

      @PreUpdate
      fun preUpdate() {
            updatedAt = LocalDateTime.now()
      }

}