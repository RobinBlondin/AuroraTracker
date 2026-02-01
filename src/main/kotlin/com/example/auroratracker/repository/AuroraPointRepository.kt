package com.example.auroratracker.repository

import com.example.auroratracker.entity.AuroraPoint
import org.springframework.data.jpa.repository.JpaRepository

interface AuroraPointRepository: JpaRepository<AuroraPoint, Long>