package com.example.studyreader.data

import android.content.Context
import android.util.AtomicFile
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

data class Flashcard(
  val id: String = UUID.randomUUID().toString(),
  val question: String,
  val answer: String,
)

enum class TutorMessageRole {
  Student,
  Tutor,
}

data class TutorMessage(
  val id: String = UUID.randomUUID().toString(),
  val role: TutorMessageRole,
  val text: String,
  val createdAt: Long = System.currentTimeMillis(),
)

data class StudySet(
  val id: String,
  val title: String,
  val studyText: String,
  val explanation: String,
  val flashcards: List<Flashcard>,
  val tutorMessages: List<TutorMessage> = emptyList(),
  val createdAt: Long,
  val updatedAt: Long,
)

interface StudySetStore {
  suspend fun loadStudySets(): List<StudySet>

  suspend fun saveStudySet(studySet: StudySet): List<StudySet>

  suspend fun deleteStudySet(id: String): List<StudySet>
}

class FileStudySetStore(context: Context) : StudySetStore {
  private val atomicFile = AtomicFile(context.filesDir.resolve(STUDY_SETS_FILE_NAME))
  private val fileMutex = Mutex()

  override suspend fun loadStudySets(): List<StudySet> =
    withContext(Dispatchers.IO) {
      fileMutex.withLock { readStudySets() }
    }

  override suspend fun saveStudySet(studySet: StudySet): List<StudySet> =
    withContext(Dispatchers.IO) {
      fileMutex.withLock {
        val updatedSets =
          (readStudySets().filterNot { it.id == studySet.id } + studySet)
            .sortedByDescending(StudySet::updatedAt)
        writeStudySets(updatedSets)
        updatedSets
      }
    }

  override suspend fun deleteStudySet(id: String): List<StudySet> =
    withContext(Dispatchers.IO) {
      fileMutex.withLock {
        val updatedSets = readStudySets().filterNot { it.id == id }
        writeStudySets(updatedSets)
        updatedSets
      }
    }

  private fun readStudySets(): List<StudySet> {
    if (!atomicFile.baseFile.exists()) return emptyList()
    val raw = atomicFile.openRead().bufferedReader(Charsets.UTF_8).use { it.readText() }
    if (raw.isBlank()) return emptyList()
    val array = JSONObject(raw).optJSONArray("studySets") ?: return emptyList()
    return buildList {
      for (index in 0 until array.length()) {
        array.optJSONObject(index)?.toStudySet()?.let(::add)
      }
    }.sortedByDescending(StudySet::updatedAt)
  }

  private fun writeStudySets(studySets: List<StudySet>) {
    val payload =
      JSONObject()
        .put("version", STUDY_SETS_VERSION)
        .put("studySets", JSONArray().apply { studySets.forEach { put(it.toJson()) } })
        .toString()

    val output = atomicFile.startWrite()
    try {
      output.write(payload.toByteArray(Charsets.UTF_8))
      output.flush()
      atomicFile.finishWrite(output)
    } catch (error: Exception) {
      atomicFile.failWrite(output)
      throw error
    }
  }
}

fun deriveStudySetTitle(studyText: String): String {
  val firstLine = studyText.lineSequence().firstOrNull(String::isNotBlank).orEmpty().trim()
  if (firstLine.isBlank()) return "Untitled study set"
  return if (firstLine.length <= MAX_AUTOMATIC_TITLE_LENGTH) {
    firstLine
  } else {
    firstLine.take(MAX_AUTOMATIC_TITLE_LENGTH).trimEnd() + "..."
  }
}

private fun StudySet.toJson(): JSONObject =
  JSONObject()
    .put("id", id)
    .put("title", title)
    .put("studyText", studyText)
    .put("explanation", explanation)
    .put("createdAt", createdAt)
    .put("updatedAt", updatedAt)
    .put(
      "flashcards",
      JSONArray().apply {
        flashcards.forEach { flashcard ->
          put(
            JSONObject()
              .put("id", flashcard.id)
              .put("question", flashcard.question)
              .put("answer", flashcard.answer),
          )
        }
      },
    )
    .put(
      "tutorMessages",
      JSONArray().apply {
        tutorMessages.forEach { message ->
          put(
            JSONObject()
              .put("id", message.id)
              .put("role", message.role.name)
              .put("text", message.text)
              .put("createdAt", message.createdAt),
          )
        }
      },
    )

private fun JSONObject.toStudySet(): StudySet? =
  runCatching {
      val cardsArray = optJSONArray("flashcards") ?: JSONArray()
      val flashcards =
        buildList {
          for (index in 0 until cardsArray.length()) {
            val card = cardsArray.optJSONObject(index) ?: continue
            val question = card.optString("question").trim()
            val answer = card.optString("answer").trim()
            if (question.isNotBlank() && answer.isNotBlank()) {
              add(
                Flashcard(
                  id = card.optString("id").ifBlank { UUID.randomUUID().toString() },
                  question = question,
                  answer = answer,
                ),
              )
            }
          }
        }

      val messagesArray = optJSONArray("tutorMessages") ?: JSONArray()
      val tutorMessages =
        buildList {
          for (index in 0 until messagesArray.length()) {
            val message = messagesArray.optJSONObject(index) ?: continue
            val text = message.optString("text").trim()
            val role = runCatching { TutorMessageRole.valueOf(message.optString("role")) }.getOrNull()
            if (text.isNotBlank() && role != null) {
              add(
                TutorMessage(
                  id = message.optString("id").ifBlank { UUID.randomUUID().toString() },
                  role = role,
                  text = text,
                  createdAt = message.optLong("createdAt", System.currentTimeMillis()),
                ),
              )
            }
          }
        }

      StudySet(
        id = getString("id"),
        title = getString("title"),
        studyText = optString("studyText"),
        explanation = optString("explanation"),
        flashcards = flashcards,
        tutorMessages = tutorMessages,
        createdAt = optLong("createdAt"),
        updatedAt = optLong("updatedAt"),
      )
    }
    .getOrNull()

private const val STUDY_SETS_FILE_NAME = "study_sets.json"
private const val STUDY_SETS_VERSION = 2
private const val MAX_AUTOMATIC_TITLE_LENGTH = 48
