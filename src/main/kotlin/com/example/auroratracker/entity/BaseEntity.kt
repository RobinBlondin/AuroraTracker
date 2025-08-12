package com.example.auroratracker.entity

import jakarta.persistence.MappedSuperclass
import jakarta.persistence.PrePersist
import jakarta.persistence.PreUpdate
import java.time.LocalDateTime

@MappedSuperclass
class BaseEntity {
      var id: Long? = null
      var createdAt: LocalDateTime? = null
      var updatedAt: LocalDateTime? = null

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