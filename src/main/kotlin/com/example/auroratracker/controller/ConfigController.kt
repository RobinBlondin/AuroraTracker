package com.example.auroratracker.controller

import com.example.auroratracker.config.EnvConfig
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class ConfigController(
      private val env: EnvConfig,
) {

      @GetMapping("/config.js", produces = ["application/javascript"])
      fun config(): String = """
    self.APP_CONFIG = {
      apiKey: "${env.firebase.apiKey}",
      authDomain: "${env.firebase.authDomain}",
      projectId: "${env.firebase.projectId}",
      storageBucket: "${env.firebase.storageBucket}",
      messagingSenderId: "${env.firebase.messagingSenderId}",
      appId: "${env.firebase.appId}",
      measurementId: "${env.firebase.measurementId}"
    };
  """.trimIndent()

      @GetMapping("/keys.js", produces = ["application/javascript"])
      fun keys(): String = """
    self.APP_KEYS = {
      webPushVapidKey: "${env.vapid.publicKey}",
      firebaseVapidKey: "${env.firebase.vapidKey}",
      secretKey: "${env.secrets.key}"
    };
  """.trimIndent()
}