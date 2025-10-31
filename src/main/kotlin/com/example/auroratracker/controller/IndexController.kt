package com.example.auroratracker.controller

import com.example.auroratracker.service.NotificationService
import com.example.auroratracker.service.TrackingService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController(
      private val notificationService: NotificationService,
      private val trackingService: TrackingService,
) {
      @GetMapping("/")
      fun index(model: Model): String {
            val kp = trackingService.getKpIndex(true)

            model.addAttribute("kp", kp)

            return "index"
      }
}