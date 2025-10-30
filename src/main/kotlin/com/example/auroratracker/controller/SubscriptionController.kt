package com.example.auroratracker.controller

import com.example.auroratracker.dto.SubscriptionDto
import com.example.auroratracker.dto.SubscriptionLiteDto
import com.example.auroratracker.service.SubscriptionService
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDateTime

@RestController
@RequestMapping(value = ["/api/subscriptions"])
class SubscriptionController(
      private val subscriptionService: SubscriptionService
) {
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()
      private val secret = dotenv["SECRET_KEY"]

      @PostMapping("subscribe")
      fun subscribe(
            @RequestHeader("X-Request-ID", required = false) providedSecret: String?,
            @RequestBody dto: SubscriptionDto): ResponseEntity<SubscriptionDto> {
            if (secret != providedSecret) return ResponseEntity(HttpStatus.FORBIDDEN)

            if (subscriptionService.checkIfSubExists(dto)) {
                  val sub = subscriptionService.getSubByUserId(dto.userId!!).orElse(null) ?: return ResponseEntity(
                        HttpStatus.NOT_FOUND
                  )

                  if(LocalDateTime.now().isBefore(sub.updatedAt?.plusMinutes(1) ?: LocalDateTime.now())) {
                        val headers = org.springframework.http.HttpHeaders()
                        headers.add("Retry-After", "60")
                        return ResponseEntity(null, headers, HttpStatus.TOO_MANY_REQUESTS)
                  }

                  subscriptionService.updateLonAndLat(sub)
                  return ResponseEntity.ok(sub)
            }

            val sub = subscriptionService.saveSub(dto)
            return ResponseEntity.ok(sub)
      }

      @DeleteMapping("unsubscribe/{userId}")
      fun unsubscribe(
            @RequestHeader("X-Request-ID", required = false) providedSecret: String?,
            @PathVariable userId: String
      ): ResponseEntity<Boolean> {
            if (secret != providedSecret) return ResponseEntity(HttpStatus.FORBIDDEN)

            val deleted = subscriptionService.deleteSubByUserId(userId)
            return ResponseEntity.ok(deleted)
      }

      @GetMapping("/{userId}")
      fun getSubscription(
            @RequestHeader("X-Request-ID", required = false) providedSecret: String?,
            @PathVariable userId: String
      ): ResponseEntity<SubscriptionLiteDto> {
            if (secret != providedSecret) return ResponseEntity(HttpStatus.FORBIDDEN)

            val sub = subscriptionService.getSubByUserId(userId)
            if (sub.isPresent) {
                  return ResponseEntity.ok(subscriptionService.toLiteDto(sub.get()))
            }
            return ResponseEntity(HttpStatus.NOT_FOUND)
      }
}