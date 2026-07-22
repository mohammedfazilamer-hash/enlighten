package com.example.studyreader.data

import android.content.Context
import android.net.Uri
import android.os.StatFs
import android.provider.OpenableColumns
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

const val ON_DEVICE_MODEL_NAME = "Gemma 3 1B"
const val ON_DEVICE_MODEL_FILE_NAME = "gemma3-1b-it-int4.litertlm"
const val ON_DEVICE_MODEL_PAGE =
  "https://huggingface.co/litert-community/Gemma3-1B-IT/blob/main/gemma3-1b-it-int4.litertlm"

private const val MINIMUM_MODEL_BYTES = 400L * 1024L * 1024L
private const val COPY_BUFFER_BYTES = 1024 * 1024
private const val STORAGE_HEADROOM_BYTES = 768L * 1024L * 1024L

data class OnDeviceModelStatus(
  val installed: Boolean,
  val sizeBytes: Long = 0,
)

class OnDeviceModelNotInstalledException :
  IllegalStateException("Install the phone AI model in Settings before using on-device AI.")

class OnDeviceStudyTutor(private val context: Context) : StudyTutor, AutoCloseable {
  private val modelDirectory = File(context.noBackupFilesDir, "ai-models")
  private val modelFile = File(modelDirectory, ON_DEVICE_MODEL_FILE_NAME)
  private val partialModelFile = File(modelDirectory, "$ON_DEVICE_MODEL_FILE_NAME.part")
  private val backupModelFile = File(modelDirectory, "$ON_DEVICE_MODEL_FILE_NAME.backup")
  private val cacheDirectory = File(context.noBackupFilesDir, "litert-lm-cache")
  private val engineMutex = Mutex()
  private var engine: Engine? = null

  fun modelStatus(): OnDeviceModelStatus =
    OnDeviceModelStatus(
      installed = modelFile.isFile && modelFile.length() >= MINIMUM_MODEL_BYTES,
      sizeBytes = modelFile.takeIf(File::isFile)?.length() ?: 0,
    )

  suspend fun importModel(uri: Uri, onProgress: (Float?) -> Unit): OnDeviceModelStatus =
    withContext(Dispatchers.IO) {
      modelDirectory.mkdirs()
      val expectedBytes = queryContentSize(uri)
      val displayName = queryDisplayName(uri)
      require(displayName == null || displayName.endsWith(".litertlm", ignoreCase = true)) {
        "Choose a .litertlm model file."
      }
      val availableBytes = StatFs(modelDirectory.absolutePath).availableBytes
      if (expectedBytes != null && availableBytes < expectedBytes + STORAGE_HEADROOM_BYTES) {
        throw IllegalStateException("Not enough phone storage for the AI model.")
      }

      partialModelFile.delete()
      try {
        val input = context.contentResolver.openInputStream(uri)
          ?: throw IllegalStateException("The selected model file could not be opened.")
        var copiedBytes = 0L
        input.use { source ->
          FileOutputStream(partialModelFile).buffered().use { destination ->
            val buffer = ByteArray(COPY_BUFFER_BYTES)
            while (true) {
              val count = source.read(buffer)
              if (count < 0) break
              destination.write(buffer, 0, count)
              copiedBytes += count
              onProgress(expectedBytes?.takeIf { it > 0 }?.let { copiedBytes.toFloat() / it })
            }
          }
        }
        require(copiedBytes >= MINIMUM_MODEL_BYTES) {
          "That file is too small to be the $ON_DEVICE_MODEL_NAME phone model."
        }

        engineMutex.withLock {
          closeEngine()
          cacheDirectory.deleteRecursively()
          backupModelFile.delete()
          if (modelFile.exists()) {
            Files.move(modelFile.toPath(), backupModelFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
          }
          try {
            Files.move(partialModelFile.toPath(), modelFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            engine = createEngine()
            backupModelFile.delete()
          } catch (error: Throwable) {
            closeEngine()
            modelFile.delete()
            if (backupModelFile.exists()) {
              Files.move(backupModelFile.toPath(), modelFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            throw IllegalStateException("That model could not start on this phone.", error)
          }
        }
        onProgress(1f)
        modelStatus()
      } catch (error: Throwable) {
        partialModelFile.delete()
        throw error
      }
    }

  suspend fun removeModel() =
    withContext(Dispatchers.IO) {
      engineMutex.withLock {
        closeEngine()
        partialModelFile.delete()
        backupModelFile.delete()
        cacheDirectory.deleteRecursively()
        if (modelFile.exists() && !modelFile.delete()) {
          throw IllegalStateException("The phone AI model could not be removed.")
        }
      }
    }

  override suspend fun checkStatus(serverUrl: String): OllamaStatus {
    val status = modelStatus()
    return OllamaStatus(
      modelInstalled = status.installed,
      modelNames = if (status.installed) listOf(ON_DEVICE_MODEL_NAME) else emptyList(),
    )
  }

  override suspend fun explain(serverUrl: String, studyText: String): String =
    generate(buildStudyPrompt(studyText), temperature = 0.2)

  override suspend fun generateFlashcards(serverUrl: String, studyText: String): List<Flashcard> =
    parseFlashcards(generate(buildOnDeviceFlashcardPrompt(studyText), temperature = 0.1))

  override suspend fun askQuestion(
    serverUrl: String,
    studyText: String,
    history: List<TutorMessage>,
    question: String,
  ): String = generate(buildTutorQuestionPrompt(studyText, history, question), temperature = 0.2)

  private suspend fun generate(prompt: String, temperature: Double): String =
    withContext(Dispatchers.Default) {
      if (!modelStatus().installed) throw OnDeviceModelNotInstalledException()
      engineMutex.withLock {
        val activeEngine = engine ?: createEngine().also { engine = it }
        val conversationConfig =
          ConversationConfig(
            samplerConfig = SamplerConfig(topK = 32, topP = 0.9, temperature = temperature),
          )
        activeEngine.createConversation(conversationConfig).use { conversation ->
          conversation.sendMessage(prompt).contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString(separator = "") { it.text }
            .trim()
            .ifBlank {
            throw IllegalStateException("The phone AI returned an empty response.")
          }
        }
      }
    }

  private fun createEngine(): Engine {
    cacheDirectory.mkdirs()
    return Engine(
      EngineConfig(
        modelPath = modelFile.absolutePath,
        backend = Backend.CPU(),
        cacheDir = cacheDirectory.absolutePath,
      ),
    ).also(Engine::initialize)
  }

  private fun queryContentSize(uri: Uri): Long? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.SIZE), null, null, null)?.use { cursor ->
      if (!cursor.moveToFirst() || cursor.isNull(0)) null else cursor.getLong(0)
    }

  private fun queryDisplayName(uri: Uri): String? =
    context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
      if (!cursor.moveToFirst() || cursor.isNull(0)) null else cursor.getString(0)
    }

  private fun closeEngine() {
    engine?.close()
    engine = null
  }

  override fun close() {
    closeEngine()
  }
}
