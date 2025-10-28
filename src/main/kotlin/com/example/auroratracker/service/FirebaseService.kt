package com.example.auroratracker.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.Notification
import io.github.cdimascio.dotenv.Dotenv
import org.springframework.stereotype.Service

@Service
class FirebaseService {
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()

      init {
            initializeFirebase()
      }

      private fun initializeFirebase() {
            val json = dotenv["FIREBASE_SERVICE_ACCOUNT"]
                  ?: ""

            println(json)

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
            val notification = Notification.builder().setTitle("Aurora Alert").setBody("Aurora activity has been detected near your location!").build()

            val message = Message.builder()
                  .setToken(token)
                  .setNotification(notification)
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