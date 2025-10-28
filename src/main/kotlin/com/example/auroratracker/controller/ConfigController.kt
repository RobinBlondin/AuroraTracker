package com.example.auroratracker.controller

import io.github.cdimascio.dotenv.Dotenv
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ConfigController {
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()

      @GetMapping("/config.js", produces = ["application/javascript"])
      fun config(): String = """
    self.APP_CONFIG = {
      apiKey: "${dotenv.get("FIREBASE_API_KEY")}",
      authDomain: "${dotenv.get("FIREBASE_AUTH_DOMAIN")}",
      projectId: "${dotenv.get("FIREBASE_PROJECT_ID")}",
      storageBucket: "${dotenv.get("FIREBASE_STORAGE_BUCKET")}",
      messagingSenderId: "${dotenv.get("FIREBASE_MESSAGING_SENDER_ID")}",
      appId: "${dotenv.get("FIREBASE_APP_ID")}",
      measurementId: "${dotenv.get("FIREBASE_MEASUREMENT_ID")}"
    };
  """.trimIndent()

      @GetMapping("/keys.js", produces = ["application/javascript"])
      fun keys(): String = """
    self.APP_KEYS = {
      webPushVapidKey: "${dotenv.get("VAPID_PUBLIC_KEY")}",
      firebaseVapidKey: "${dotenv.get("FIREBASE_VAPID_KEY")}",
    };
  """.trimIndent()
}