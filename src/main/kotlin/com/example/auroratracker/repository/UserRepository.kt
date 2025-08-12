package com.example.auroratracker.repository

import com.example.auroratracker.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository: JpaRepository<User, Long>