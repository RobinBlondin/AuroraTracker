package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.springframework.stereotype.Service

@Service
class FirebaseService(
      private val env: EnvConfig
) {

      init {
            initializeFirebase()
      }

      private fun initializeFirebase() {
            val json = env.firebase.serviceAccount
            val credentials = GoogleCredentials.fromStream(json.byteInputStream(Charsets.UTF_8))
            val options = FirebaseOptions.builder()
                  .setCredentials(credentials)
                  .build()

            if (FirebaseApp.getApps().isEmpty()) {
                  FirebaseApp.initializeApp(options)
                  println("Firebase initialized from environment variable")
            } else {
                  println("Firebase already initialized")
            }
      }

      fun sendNotification(token: String) {
            val message = Message.builder()
                  .setToken(token)
                  .putData("title", "Aurora Alert")
                  .putData("body", "Aurora activity detected near your location!")
                  .putData("image", "/images/icon-192.png")
                  .build()

            try {
                  val response = FirebaseMessaging.getInstance().send(message)
                  println("Sent Firebase message: $response")
            } catch (e: Exception) {
                  e.printStackTrace()
                  println("Failed to send Firebase notification: ${e.message}")
            }
      }
}