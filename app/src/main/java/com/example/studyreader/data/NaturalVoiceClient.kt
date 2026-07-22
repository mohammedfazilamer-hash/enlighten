package com.example.studyreader.data

import java.io.File
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

private const val NATURAL_VOICE_PORT = 11435
private const val NATURAL_VOICE_MODEL = "kokoro-en-v0_19"
private const val MAX_AUDIO_BYTES = 12 * 1024 * 1024

data class NaturalVoiceStatus(val model: String, val voiceCount: Int)

class NaturalVoiceClient(private val cacheDirectory: File) {
  suspend fun checkStatus(ollamaServerUrl: String): NaturalVoiceStatus =
    withContext(Dispatchers.IO) {
      val connection = openConnection(naturalVoiceBaseUrl(ollamaServerUrl) + "/health", "GET")
      try {
        val payload = JSONObject(readResponse(connection))
        val status = payload.optString("status")
        if (status != "ready") throw IllegalStateException("The natural voice service is not ready.")
        NaturalVoiceStatus(
          model = payload.optString("model", NATURAL_VOICE_MODEL),
          voiceCount = payload.optJSONArray("voices")?.length() ?: 0,
        )
      } finally {
        connection.disconnect()
      }
    }

  suspend fun synthesize(
    ollamaServerUrl: String,
    text: String,
    voiceId: Int,
    speed: Float,
  ): File =
    withContext(Dispatchers.IO) {
      val cleanText = text.trim()
      require(cleanText.isNotBlank()) { "There is no text to read." }
      val cacheKey = "$NATURAL_VOICE_MODEL|$voiceId|${"%.2f".format(java.util.Locale.US, speed)}|$cleanText"
      val audioDirectory = File(cacheDirectory, "natural_voice").apply { mkdirs() }
      val target = File(audioDirectory, sha256(cacheKey) + ".wav")
      if (target.isFile && target.length() > 44L) return@withContext target

      val connection = openConnection(naturalVoiceBaseUrl(ollamaServerUrl) + "/synthesize", "POST")
      try {
        val requestBody =
          JSONObject()
            .put("text", cleanText)
            .put("voice_id", voiceId)
            .put("speed", speed.coerceIn(0.6f, 1.5f).toDouble())
            .toString()
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(requestBody) }
        val status = connection.responseCode
        if (status !in 200..299) {
          val message = connection.errorStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
          val serverMessage = runCatching { JSONObject(message).optString("error") }.getOrNull()
          throw IllegalStateException(serverMessage?.takeIf(String::isNotBlank) ?: "Natural voice request failed ($status).")
        }
        val declaredLength = connection.contentLengthLong
        if (declaredLength > MAX_AUDIO_BYTES) throw IllegalStateException("Natural voice audio was unexpectedly large.")
        val temporary = File(audioDirectory, target.name + ".tmp")
        connection.inputStream.use { input ->
          temporary.outputStream().buffered().use { output ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            var total = 0L
            while (true) {
              val count = input.read(buffer)
              if (count < 0) break
              total += count
              if (total > MAX_AUDIO_BYTES) {
                temporary.delete()
                throw IllegalStateException("Natural voice audio was unexpectedly large.")
              }
              output.write(buffer, 0, count)
            }
          }
        }
        if (temporary.length() <= 44L) {
          temporary.delete()
          throw IllegalStateException("Natural voice returned empty audio.")
        }
        if (!temporary.renameTo(target)) {
          temporary.copyTo(target, overwrite = true)
          temporary.delete()
        }
        target
      } finally {
        connection.disconnect()
      }
    }

  private fun openConnection(url: String, method: String): HttpURLConnection =
    (URL(url).openConnection() as HttpURLConnection).apply {
      connectTimeout = 5_000
      readTimeout = 45_000
      requestMethod = method
      setRequestProperty("Accept", if (method == "GET") "application/json" else "audio/wav")
    }

  private fun readResponse(connection: HttpURLConnection): String {
    val status = connection.responseCode
    val stream = if (status in 200..299) connection.inputStream else connection.errorStream
    val response = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
    if (status !in 200..299) {
      val message = runCatching { JSONObject(response).optString("error") }.getOrNull()
      throw IllegalStateException(message?.takeIf(String::isNotBlank) ?: "Natural voice request failed ($status).")
    }
    return response
  }
}

fun naturalVoiceBaseUrl(ollamaServerUrl: String): String {
  val ollamaUri = URI(normalizeOllamaBaseUrl(ollamaServerUrl))
  return URI("http", null, ollamaUri.host, NATURAL_VOICE_PORT, null, null, null).toString().trimEnd('/')
}

private fun sha256(value: String): String =
  MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { byte -> "%02x".format(byte) }
