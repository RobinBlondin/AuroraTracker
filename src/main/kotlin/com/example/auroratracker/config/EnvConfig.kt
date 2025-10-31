package com.example.auroratracker.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "env")
data class EnvConfig(
      val api: ApiConfig,
      val db: DbConfig,
      val vapid: VapidConfig,
      val firebase: FirebaseConfig,
      val secrets: SecretConfig
) {
      data class ApiConfig(val url: Urls) {
            data class Urls(val noaa: String, val kp: String, val met: String)
      }
      data class DbConfig(val url: String, val user: String, val password: String)
      data class VapidConfig(val subject: String, val publicKey: String, val privateKey: String)
      data class FirebaseConfig(
            val serviceAccount: String,
            val vapidKey: String,
            val apiKey: String,
            val authDomain: String,
            val projectId: String,
            val storageBucket: String,
            val messagingSenderId: String,
            val appId: String,
            val measurementId: String
      )
      data class SecretConfig(val push: String, val key: String)
}
