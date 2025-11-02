package com.example.auroratracker.controller

import com.example.auroratracker.enums.FeedbackState
import com.example.auroratracker.service.FeedbackService
import com.example.auroratracker.service.TrackingService
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.support.RedirectAttributes
import java.time.LocalDateTime
import java.util.*

@Controller
class WebController(
      private val trackingService: TrackingService,
      private val feedbackService: FeedbackService,
) {
      @GetMapping("/")
      fun index(model: Model): String {
            val kp = trackingService.getKpIndex(true)

            model.addAttribute("kp", kp)

            return "index"
      }

      @GetMapping("/feedback/{token}")
      fun getFeedback(@PathVariable("token") token: UUID, redirectAttributes: RedirectAttributes, model: Model): String {
            val feedback = feedbackService.getFeedback(token)

            if(feedback == null) {
                  redirectAttributes.addFlashAttribute("feedbackNotFound", true)
                  return "redirect:/"
            }

            if(LocalDateTime.now().isAfter(feedback.expires)) {
                  redirectAttributes.addFlashAttribute("feedbackExpired", true)
                  return "redirect:/"
            }

            model.addAttribute("feedbackToken", feedback.token)

            return "feedback"
      }

      @PostMapping("/feedback/{token}")
      fun updateFeedback(
            @PathVariable("token") token: UUID,
            @RequestParam feedbackState: FeedbackState,
            redirectAttributes: RedirectAttributes,
            model: Model): String {

            val feedback = feedbackService.getFeedback(token)

            if(feedback == null) {
                  redirectAttributes.addFlashAttribute("feedbackNotFound", true)
                  return "redirect:/"
            }

            if(LocalDateTime.now().isAfter(feedback.expires)) {
                  redirectAttributes.addFlashAttribute("feedbackExpired", true)
                  return "redirect:/"
            }

            feedback.state = feedbackState
            feedback.expires = LocalDateTime.now()
            feedbackService.saveFeedback(feedback)
            redirectAttributes.addFlashAttribute("feedbackUpdated", true)
            return "redirect:/"
      }
}