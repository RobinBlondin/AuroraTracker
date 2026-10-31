package com.example.auroratracker.service

import com.example.auroratracker.config.EnvConfig
import com.example.auroratracker.dto.SubscriptionDto
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
    private val env: EnvConfig,
    private val subscriptionService: SubscriptionService
) {

    init {
        Security.addProvider(BouncyCastleProvider())
    }

    fun sendManagementNotification(endpoint: String, p256dh: String, auth: String, url: String) {
        val payload = com.fasterxml.jackson.module.kotlin.jacksonObjectMapper().writeValueAsString(
            mapOf(
                "title" to "Aurora Tracker",
                "body" to "Tap to manage your alert preferences. Only continue if you requested this.",
                "url" to url,
                "type" to "management"
            )
        )
        val pushService = PushService(env.vapid.publicKey, env.vapid.privateKey, env.vapid.subject)
        val response = pushService.send(Notification(endpoint, p256dh, auth, payload.toByteArray(Charsets.UTF_8), 600))
        if (response.statusLine.statusCode !in 200..299) {
            throw IllegalStateException("Management notification delivery failed")
        }
    }

    fun sendNotification(endpoint: String?, p256dh: String?, auth: String?, cloudCover: String, sub: SubscriptionDto): Boolean {
        val publicKey = env.vapid.publicKey
        val privateKey = env.vapid.privateKey
        val subject = env.vapid.subject
        val pushService = PushService(publicKey, privateKey, subject)

        try {
            val path = Paths.get(javaClass.classLoader.getResource("message.json")!!.toURI())
            var payload = Files.readString(path)
            payload = payload.replace("CLOUD_COVER", cloudCover)

            val subscription = Subscription(endpoint, Subscription.Keys(p256dh, auth))
            val notification = Notification(subscription, payload)
            val response = pushService.send(notification)

            when (val status = response.statusLine.statusCode) {
                in 200..299 -> {
                    println("Push sent successfully")
                    return true
                }
                404, 410 -> {
                    println("Subscription expired: $endpoint")
                    subscriptionService.handleInvalidPushSubscription(sub)
                    return false
                }

                429 -> {
                    println("Rate limited, retry later")
                    return false
                }
                else -> {
                    println("Push failed: $status")
                    return false
                }
            }
        } catch (e: Exception) {
            println("Push  Error: ${e.message}")
            e.printStackTrace()
            return false
        }
    }
}