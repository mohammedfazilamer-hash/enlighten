package com.example.studyreader.data

import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

const val DEFAULT_OLLAMA_URL = "http://192.168.1.100:11434"
const val STUDY_MODEL = "llama3.2:3b"

data class OllamaStatus(val modelInstalled: Boolean, val modelNames: List<String>)

interface StudyTutor {
  suspend fun checkStatus(serverUrl: String): OllamaStatus

  suspend fun explain(serverUrl: String, studyText: String): String

  suspend fun generateFlashcards(serverUrl: String, studyText: String): List<Flashcard>

  suspend fun askQuestion(
    serverUrl: String,
    studyText: String,
    history: List<TutorMessage>,
    question: String,
  ): String
}

class OllamaClient : StudyTutor {
  override suspend fun checkStatus(serverUrl: String): OllamaStatus =
    withContext(Dispatchers.IO) {
      val payload = request(normalizeOllamaBaseUrl(serverUrl) + "/api/tags")
      val models = payload.optJSONArray("models")
      val names =
        buildList {
          if (models != null) {
            for (index in 0 until models.length()) {
              models.optJSONObject(index)?.optString("name")?.takeIf(String::isNotBlank)?.let(::add)
            }
          }
        }
      OllamaStatus(
        modelInstalled = names.any { it == STUDY_MODEL || it.startsWith("llama3.2:") },
        modelNames = names,
      )
    }

  override suspend fun explain(serverUrl: String, studyText: String): String =
    withContext(Dispatchers.IO) {
      val body =
        JSONObject()
          .put("model", STUDY_MODEL)
          .put("prompt", buildStudyPrompt(studyText))
          .put("stream", false)
          .put("keep_alive", "10m")
          .put("options", JSONObject().put("temperature", 0.2))

      val payload = request(normalizeOllamaBaseUrl(serverUrl) + "/api/generate", body)
      payload.optString("response").trim().ifBlank {
        throw IllegalStateException("Ollama returned an empty explanation.")
      }
    }

  override suspend fun generateFlashcards(serverUrl: String, studyText: String): List<Flashcard> =
    withContext(Dispatchers.IO) {
      val body =
        JSONObject()
          .put("model", STUDY_MODEL)
          .put("prompt", buildFlashcardPrompt(studyText))
          .put("stream", false)
          .put("keep_alive", "10m")
          .put("format", "json")
          .put("options", JSONObject().put("temperature", 0.15))

      val payload = request(normalizeOllamaBaseUrl(serverUrl) + "/api/generate", body)
      parseFlashcards(payload.optString("response"))
    }

  override suspend fun askQuestion(
    serverUrl: String,
    studyText: String,
    history: List<TutorMessage>,
    question: String,
  ): String =
    withContext(Dispatchers.IO) {
      val body =
        JSONObject()
          .put("model", STUDY_MODEL)
          .put("prompt", buildTutorQuestionPrompt(studyText, history, question))
          .put("stream", false)
          .put("keep_alive", "10m")
          .put("options", JSONObject().put("temperature", 0.2))

      val payload = request(normalizeOllamaBaseUrl(serverUrl) + "/api/generate", body)
      payload.optString("response").trim().ifBlank {
        throw IllegalStateException("The tutor returned an empty answer.")
      }
    }

  private fun request(url: String, body: JSONObject? = null): JSONObject {
    val connection = (URL(url).openConnection() as HttpURLConnection)
    try {
      connection.connectTimeout = 10_000
      connection.readTimeout = 180_000
      connection.requestMethod = if (body == null) "GET" else "POST"
      connection.setRequestProperty("Accept", "application/json")

      if (body != null) {
        connection.doOutput = true
        connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
        connection.outputStream.bufferedWriter(Charsets.UTF_8).use { it.write(body.toString()) }
      }

      val status = connection.responseCode
      val responseStream = if (status in 200..299) connection.inputStream else connection.errorStream
      val responseText = responseStream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
      if (status !in 200..299) {
        val serverMessage = runCatching { JSONObject(responseText).optString("error") }.getOrNull()
        throw IllegalStateException(serverMessage?.takeIf(String::isNotBlank) ?: "Ollama request failed ($status).")
      }
      return JSONObject(responseText)
    } finally {
      connection.disconnect()
    }
  }
}

fun normalizeOllamaBaseUrl(value: String): String {
  val raw = value.trim().trimEnd('/')
  require(raw.isNotBlank()) { "Enter your computer's Ollama address." }
  val withScheme = if (raw.contains("://")) raw else "http://$raw"
  val uri = runCatching { URI(withScheme) }.getOrElse { throw IllegalArgumentException("Enter a valid Ollama address.") }
  require(uri.scheme == "http" || uri.scheme == "https") { "The address must start with http:// or https://." }
  require(!uri.host.isNullOrBlank()) { "Enter a valid computer IP address." }
  require(uri.path.isNullOrBlank() && uri.query == null && uri.fragment == null) {
    "Enter only the server address, without /api/generate."
  }
  return URI(uri.scheme, null, uri.host, uri.port, null, null, null).toString().trimEnd('/')
}

fun buildStudyPrompt(studyText: String): String =
  """
  You are a patient study tutor. Explain only the study text below in clear, simple language.
  Do not assume the learner already knows the subject. If the text is unclear, say what is unclear.

  Return plain text only. Start each section with these exact headings on separate lines:
  1. Simple explanation
  2. Important terms
  3. Key points
  4. Three quiz questions

  Do not rename or omit a heading. Keep the explanation focused, do not use Markdown tables,
  and do not provide the answers to the quiz questions.

  Study text:
  ${studyText.trim()}
  """.trimIndent()

fun buildFlashcardPrompt(studyText: String): String =
  """
  Create exactly 6 useful study flashcards using only the study text below.
  Questions must test the most important ideas, not minor wording details.
  Answers must be concise, clear, and understandable without extra context.

  Return only a JSON object in this exact structure:
  {"flashcards":[{"question":"Question","answer":"Answer"}]}

  Study text:
  ${studyText.trim()}
  """.trimIndent()

fun buildTutorQuestionPrompt(
  studyText: String,
  history: List<TutorMessage>,
  question: String,
): String {
  val recentConversation =
    history.takeLast(6).joinToString("\n") { message ->
      val speaker = if (message.role == TutorMessageRole.Student) "Student" else "Tutor"
      "$speaker: ${message.text.trim()}"
    }.ifBlank { "No previous questions." }
  return """
    You are a patient study tutor. Answer the student's question using the study passage below.
    Explain in simple language and keep the answer focused. If the passage does not contain enough
    information, say that clearly instead of inventing facts. Return plain text only.

    Recent conversation:
    $recentConversation

    Study passage:
    ${studyText.trim()}

    Student's new question:
    ${question.trim()}
  """.trimIndent()
}

internal fun parseFlashcards(response: String): List<Flashcard> {
  val raw = response.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
  val payload = runCatching { JSONObject(raw) }.getOrElse {
    val objectStart = raw.indexOf('{')
    val objectEnd = raw.lastIndexOf('}')
    if (objectStart < 0 || objectEnd <= objectStart) {
      throw IllegalStateException("The tutor returned flashcards in an unreadable format.")
    }
    JSONObject(raw.substring(objectStart, objectEnd + 1))
  }
  val array = payload.optJSONArray("flashcards")
    ?: throw IllegalStateException("The tutor did not return any flashcards.")
  val flashcards =
    buildList {
      for (index in 0 until array.length()) {
        val card = array.optJSONObject(index) ?: continue
        val question = card.optString("question").trim()
        val answer = card.optString("answer").trim()
        if (question.isNotBlank() && answer.isNotBlank()) {
          add(Flashcard(question = question, answer = answer))
        }
      }
    }
  return flashcards.take(10).ifEmpty {
    throw IllegalStateException("The tutor did not return any usable flashcards.")
  }
}
