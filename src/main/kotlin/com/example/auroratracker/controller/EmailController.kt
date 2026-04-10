package com.example.auroratracker.controller

import com.example.auroratracker.dto.EmailDto
import com.example.auroratracker.service.EmailService
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/email")
class EmailController(
      val emailService: EmailService
) {

      @PostMapping("send")
      fun sendEmail(@RequestBody dto: EmailDto): ResponseEntity<String> {
            val response = emailService.sendEmail(dto.to, dto.from, dto.subject, dto.content)
            return ResponseEntity(HttpStatusCode(response.statusCode)
      }
}