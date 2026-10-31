package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.dto.SubscriptionDto
import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import org.springframework.stereotype.Service

@Service
class FirebaseService(
      private val env: EnvConfig,
      private val subscriptionService: SubscriptionService
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

      fun sendManagementNotification(token: String, url: String) {
            val message = Message.builder()
                  .setToken(token)
                  .putData("title", "Aurora Tracker")
                  .putData("body", "Tap to manage your alert preferences. Only continue if you requested this.")
                  .putData("url", url)
                  .putData("type", "management")
                  .setWebpushConfig(com.google.firebase.messaging.WebpushConfig.builder().putHeader("TTL", "600").build())
                  .build()
            FirebaseMessaging.getInstance().send(message)
      }

      fun sendNotification(token: String, cloudCover: Double, sub: SubscriptionDto): Boolean {
            val message = Message.builder()
                  .setToken(token)
                  .putData("title", "Aurora Alert")
                  .putData("body", "Aurora activity detected near your location! Cloud cover: $cloudCover")
                  .putData("image", "/images/icon-192.png")
                  .build()

            try {
                  val response = FirebaseMessaging.getInstance().send(message)
                  println("Sent Firebase message: $response")
                  return true
            } catch (e: FirebaseMessagingException) {
                  when (e.messagingErrorCode) {
                        MessagingErrorCode.UNREGISTERED -> {
                              println("Firebase token expired: $token")
                              subscriptionService.handleInvalidPushSubscription(sub)
                              return false
                        }
                        MessagingErrorCode.INVALID_ARGUMENT -> {
                              println("Invalid Firebase request/token: ${e.message}")
                              return false
                        }
                        else -> {
                              println("Firebase error: ${e.messagingErrorCode}")
                              return false
                        }
                  }
            }
      }
}