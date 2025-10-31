package com.example.auroratracker.controller

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.service.FirebaseService
import com.example.auroratracker.service.SubscriptionService
import com.example.auroratracker.service.WebPushService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/push")
class PushNotificationController(
      private val subscriptionService: SubscriptionService,
      private val webPushService: WebPushService,
      private val firebaseService: FirebaseService,
      private val env: EnvConfig
) {
      @PostMapping("/all")
      fun pushAll(@RequestHeader(value = "X-Request-ID", required = false) providedSecret: String?): ResponseEntity<String> {
            if(env.secrets.push != providedSecret) return ResponseEntity(HttpStatus.UNAUTHORIZED)

            val subs = subscriptionService.getAllSubs()

            for (sub in subs) {
                  if(sub.firebaseToken != null) {
                        firebaseService.sendNotification(sub.firebaseToken!!)
                  } else {
                        webPushService.sendNotification(sub.endpoint, sub.p256dh, sub.auth)
                  }
            }
            return ResponseEntity.ok("Success")
      }

      @PostMapping("/{userId}")
      fun pushUserById(@RequestHeader(value = "X-Request-ID", required = false) providedSecret: String?, @PathVariable userId: String): ResponseEntity<String> {
            if(env.secrets.push != providedSecret) return ResponseEntity(HttpStatus.UNAUTHORIZED)

            val sub =
                  subscriptionService.getSubByUserId(userId).orElse(null) ?: return ResponseEntity.notFound().build()

            if(sub.firebaseToken != null) {
                  firebaseService.sendNotification(sub.firebaseToken!!)
            } else {
                  webPushService.sendNotification(sub.endpoint, sub.p256dh, sub.auth)
            }

            return ResponseEntity.ok("Success")
      }
}