package com.example.auroratracker.service

import com.example.auroratracker.config.CustomOkHttpClient
import kotlinx.serialization.json.Json
import okhttp3.Request
import okio.IOException
import org.springframework.stereotype.Service

@Service
class JsonService(
      val client: CustomOkHttpClient
) {
      val jsonParser: Json = Json { ignoreUnknownKeys = true }

      fun fetch(url: String): String {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                  if (!response.isSuccessful) throw IOException("Unexpected code ${response.code} from $url")
                  return response.body.string()
            }
      }

      final inline fun <reified T> parse(json: String): T {
            return jsonParser.decodeFromString(json)
      }

      final inline fun <reified T> fetchAndParse(url: String): Result<T> {
            try {
                  val response = fetch(url)
                  val result = parse<T>(response)
                  return Result.success(result)
            } catch (e: IOException) {
                  println(e.message)
                  return Result.failure(e)
            } catch (e: Exception) {
                  println("Failed to parse response from $url: ${e.message}")
                  return Result.failure(e)
            }
      }
}