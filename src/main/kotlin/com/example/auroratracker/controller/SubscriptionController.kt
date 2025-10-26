package com.example.auroratracker.controller

import com.example.auroratracker.dto.SubscriptionDto
import com.example.auroratracker.service.SubscriptionService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping(value = ["/api/subscriptions"])
class SubscriptionController(
      private val subscriptionService: SubscriptionService
) {
      @GetMapping("subscribe")
      fun subscribe(@RequestBody dto: SubscriptionDto): ResponseEntity<SubscriptionDto> {
            if(subscriptionService.checkIfSubExists(dto)) {
                  val sub = subscriptionService.getSubByUserId(dto.userId!!).orElse(null) ?: return ResponseEntity(HttpStatus.NOT_FOUND)

                  if(dto.lon != sub.lon || dto.lat != sub.lat) {
                        subscriptionService.updateLonAndLat(sub)
                        return ResponseEntity.ok(sub)
                  } else {
                        return ResponseEntity.status(HttpStatus.CONFLICT).build()
                  }
            }

            val sub = subscriptionService.saveSub(dto)
            return ResponseEntity.ok(sub)
      }

      @DeleteMapping("unsubscribe/{userId}")
      fun unsubscribe(@PathVariable userId: String): ResponseEntity<Boolean> {
            val deleted = subscriptionService.deleteSubByUserId(userId)
            return ResponseEntity.ok(deleted)
      }

      @GetMapping("/{userId}")
      fun getSubscription(@PathVariable userId: String): ResponseEntity<SubscriptionDto> {
            val sub = subscriptionService.getSubByUserId(userId)
            if(sub.isPresent) {
                  return ResponseEntity.ok(sub.get())
            }
            return ResponseEntity(HttpStatus.NOT_FOUND)
      }
}