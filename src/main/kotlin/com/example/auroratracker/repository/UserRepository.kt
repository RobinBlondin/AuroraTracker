package com.example.auroratracker.repository

import com.example.auroratracker.entity.User
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository

@Repository
interface UserRepository: JpaRepository<User, Long> {
      fun existsByEmailOrPhoneNumber(email: String, phoneNumber: String): Boolean
}