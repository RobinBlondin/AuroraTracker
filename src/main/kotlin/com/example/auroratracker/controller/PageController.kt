package com.example.auroratracker.controller

import com.example.auroratracker.service.EmailVerificationService
import com.example.auroratracker.service.ManagementSessionService
import com.example.auroratracker.service.TrackingService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@Controller
class PageController(
      private val managementSessions: ManagementSessionService,
      private val trackingService: TrackingService,
      private val emailVerificationService: EmailVerificationService
) {
    @GetMapping("/")
    fun index(model: Model): String {
        val kp = trackingService.getKpIndex(true)

        model.addAttribute("kp", kp)

        return "index"
    }

    @GetMapping("/signup")
    fun getSignupPage(): String {
        return "signup"
    }

    @GetMapping("/manage-alerts")
    fun manageSubscription(): String {
        return "manage-alerts"
    }

    @GetMapping("/verification")
    fun result(@RequestParam(defaultValue = "invalid") status: String, model: Model): String {
        model.addAttribute("status", status.takeIf { it in setOf("success", "expired", "invalid") } ?: "invalid")
        return "verification"
    }

    @GetMapping("/verification/{token}")
    fun verifyEmail(
        @PathVariable token: String,
        request: HttpServletRequest
    ): String {
        val result = emailVerificationService.confirmEmail(token)

        result.userId?.let { userId ->
            managementSessions.establish(request, userId)
        }

        return "redirect:/verification?status=${result.status}"
    }
}