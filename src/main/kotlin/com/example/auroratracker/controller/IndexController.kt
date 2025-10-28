package com.example.auroratracker.controller

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping

@Controller
class IndexController {
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()
      @GetMapping("/")
      fun index(model: Model): String {
            val vapidKey = dotenv.get("VAPID_PUBLIC_KEY") ?: ""
            val firebaseKey = dotenv.get("FIREBASE_VAPID_KEY") ?: ""
            val firebaseApiKey = dotenv.get("FIREBASE_API_KEY") ?: ""
            val firebaseAuthDomain = dotenv.get("FIREBASE_AUTH_DOMAIN") ?: ""
            val firebaseProjectId = dotenv.get("FIREBASE_PROJECT_ID") ?: ""
            val firebaseStorageBucket = dotenv.get("FIREBASE_STORAGE_BUCKET") ?: ""
            val firebaseMessagingSenderId = dotenv.get("FIREBASE_MESSAGING_SENDER_ID") ?: ""
            val firebaseAppId = dotenv.get("FIREBASE_APP_ID") ?: ""
            val firebaseMeasurementId = dotenv.get("FIREBASE_MEASUREMENT_ID") ?: ""

            model.addAttribute("vapidKey", vapidKey)
            model.addAttribute("firebaseKey", firebaseKey)
            model.addAttribute("firebaseApiKey", firebaseApiKey)
            model.addAttribute("firebaseAuthDomain", firebaseAuthDomain)
            model.addAttribute("firebaseProjectId", firebaseProjectId)
            model.addAttribute("firebaseStorageBucket", firebaseStorageBucket)
            model.addAttribute("firebaseMessagingSenderId", firebaseMessagingSenderId)
            model.addAttribute("firebaseAppId", firebaseAppId)
            model.addAttribute("firebaseMeasurementId", firebaseMeasurementId)
            return "index"
      }
}