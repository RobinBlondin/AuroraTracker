package com.example.auroratracker.controller

import com.example.auroratracker.dto.SmsDto
import com.example.auroratracker.service.SmsService
import com.twilio.type.PhoneNumber
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/sms")
class SmsController(
      private val smsService: SmsService
) {
      @PostMapping("/send")
      fun sendSms(@RequestBody message: SmsDto): ResponseEntity<String> {
            val to = PhoneNumber(message.to)
            val from = PhoneNumber(message.from)
            smsService.sendMessage(to, from, message.message)
            return ResponseEntity.ok("SMS sent successfully")
      }
}