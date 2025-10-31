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
            val today = notificationService.countToday()
            val total = notificationService.countTotal()
            val kp = trackingService.getKpIndex()

            model.addAttribute("kp", kp)
            model.addAttribute("total", total)
            model.addAttribute("today", today)

            return "index"
      }
}