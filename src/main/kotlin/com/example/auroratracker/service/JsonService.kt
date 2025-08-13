package com.example.auroratracker.service

import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import org.springframework.stereotype.Service

@Service
class JsonService {
      val jsonParser: Json = Json { ignoreUnknownKeys = true }

      fun fetch(url: String): String {
            val client = OkHttpClient()
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                  return response.body.string()
            }
      }

      final inline fun <reified T> parse(json: String): T {
            return jsonParser.decodeFromString(json)
      }
}