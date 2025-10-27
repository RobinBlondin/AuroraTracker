package com.example.auroratracker.controller

import com.example.auroratracker.service.PushNotificationService
import com.example.auroratracker.service.SubscriptionService
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/push")
class PushNotificationController(
      private val subscriptionService: SubscriptionService,
      private val pushNotificationService: PushNotificationService,
) {

      private val dotenv = Dotenv.configure().ignoreIfMissing().load()

      @GetMapping("/all")
      fun pushAll(): ResponseEntity<String> {

            val subs = subscriptionService.getAllSubs()

            for (sub in subs) {
                  pushNotificationService.sendNotification(sub.endPoint, sub.p256dh, sub.auth, "")
            }
            return ResponseEntity.ok("Success")
      }

      @GetMapping("/{userId}")
      fun pushUserById(@PathVariable userId: String): ResponseEntity<String> {

            val sub =
                  subscriptionService.getSubByUserId(userId).orElse(null) ?: return ResponseEntity.notFound().build()

            pushNotificationService.sendNotification(sub.endPoint, sub.p256dh, sub.auth, "")

            return ResponseEntity.ok("Success")
      }
}