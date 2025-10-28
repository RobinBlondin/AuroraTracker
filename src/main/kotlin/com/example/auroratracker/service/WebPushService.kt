package com.example.auroratracker.service

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import io.github.cdimascio.dotenv.Dotenv
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Subscription
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.stereotype.Service
import java.io.ByteArrayInputStream
import java.nio.charset.StandardCharsets
import java.security.Security


@Service
class WebPushService {
      private val dotenv = Dotenv.configure().ignoreIfMissing().load()
      init {
            Security.addProvider(BouncyCastleProvider())
      }

      private val publicKey = dotenv["VAPID_PUBLIC_KEY"] ?: ""
      private val privateKey = dotenv["VAPID_PRIVATE_KEY"] ?: ""
      private val subject = dotenv["VAPID_SUBJECT"] ?: "mailto:robin.blondin@gmail.com"
      private val pushService = PushService(publicKey, privateKey, subject)

      fun sendNotification(endpoint: String?, p256dh: String?, auth: String?, message: String) {
                  try {
                        val subscription = Subscription(endpoint, Subscription.Keys(p256dh, auth))
                        val notification = Notification(subscription, message)
                        val response = pushService.send(notification)
                        println("Push response: ${response.statusLine}")
                  } catch (e: Exception) {
                        println("Push  Error: ${e.message}")
                        e.printStackTrace()
                  }
      }
}