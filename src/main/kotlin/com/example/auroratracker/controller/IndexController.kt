package com.example.auroratracker.controller

import com.example.auroratracker.service.PushNotificationService
import com.example.auroratracker.service.SubscriptionService
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController(
      private val pushNotificationService: PushNotificationService,
      private val subscriptionService: SubscriptionService
) {
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()
      @GetMapping("/")
      fun index(model: Model): String {
            val key = dotenv.get("VAPID_PUBLIC_KEY") ?: ""

            model.addAttribute("key", key)
            return "index"
      }


      @GetMapping("/push")
      fun pushNotification(model: Model): String {
            val key = dotenv.get("VAPID_PUBLIC_KEY") ?: ""
            model.addAttribute("key", key)
            val subs = subscriptionService.getAllSubs()

            for(sub in subs) {
                  pushNotificationService.sendNotification(sub.endPoint, sub.p256dh, sub.auth, "")
            }



            return "index"
      }
}