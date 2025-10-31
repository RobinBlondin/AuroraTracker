package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import nl.martijndwars.webpush.Notification
import nl.martijndwars.webpush.PushService
import nl.martijndwars.webpush.Subscription
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.springframework.stereotype.Service
import java.nio.file.Files
import java.nio.file.Paths
import java.security.Security

@Service
class WebPushService(
      private val env: EnvConfig
) {

      init {
            Security.addProvider(BouncyCastleProvider())
      }

      fun sendNotification(endpoint: String?, p256dh: String?, auth: String?) {
             val publicKey = env.vapid.publicKey
             val privateKey = env.vapid.privateKey
             val subject = env.vapid.subject
             val pushService = PushService(publicKey, privateKey, subject)

            try {
                  val path = Paths.get(javaClass.classLoader.getResource("message.json")!!.toURI())
                  val payload =  Files.readString(path)

                  val subscription = Subscription(endpoint, Subscription.Keys(p256dh, auth))
                  val notification = Notification(subscription, payload)
                  val response = pushService.send(notification)
                  println("Push response: ${response.statusLine}")
            } catch (e: Exception) {
                  println("Push  Error: ${e.message}")
                  e.printStackTrace()
            }
      }
}