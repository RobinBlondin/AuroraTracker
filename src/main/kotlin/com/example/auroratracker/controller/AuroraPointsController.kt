package com.example.auroratracker.controller

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.dto.AuroraPointDto
import com.example.auroratracker.service.AuroraPointService
import com.example.auroratracker.service.TrackingService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/points")
class AuroraPointsController(
      private val auroraPointService: AuroraPointService,
      private val env: EnvConfig
) {
      @GetMapping("all")
      fun getPoints(
            @RequestHeader(value = "X-Request-ID", required = false) secretDelivered: String
            ): ResponseEntity<List<AuroraPointDto>> {
            if(env.secrets.key != secretDelivered) return ResponseEntity(HttpStatus.UNAUTHORIZED)

            val points = auroraPointService.getAuroraPoints()
            return ResponseEntity.ok(points)
      }
}