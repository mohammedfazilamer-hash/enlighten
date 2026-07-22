package com.example.studyreader.ui.main

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.studyreader.data.AiExecutionProvider
import com.example.studyreader.data.AiProviderMode
import com.example.studyreader.data.AiProvidersUnavailableException
import com.example.studyreader.data.AiTutorCoordinator
import com.example.studyreader.data.DocumentTextExtractor
import com.example.studyreader.data.DEFAULT_OLLAMA_URL
import com.example.studyreader.data.Flashcard
import com.example.studyreader.data.OnDeviceModelNotInstalledException
import com.example.studyreader.data.OnDeviceModelStatus
import com.example.studyreader.data.OnDeviceStudyTutor
import com.example.studyreader.data.ON_DEVICE_MODEL_NAME
import com.example.studyreader.data.ScreenshotTextExtractor
import com.example.studyreader.data.StudySet
import com.example.studyreader.data.StudySetStore
import com.example.studyreader.data.StudyTutor
import com.example.studyreader.data.TutorMessage
import com.example.studyreader.data.TutorMessageRole
import com.example.studyreader.data.deriveStudySetTitle
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MainScreenUiState(
  val studyText: String = "",
  val serverUrl: String = DEFAULT_OLLAMA_URL,
  val aiProviderMode: AiProviderMode = AiProviderMode.Automatic,
  val onDeviceModelStatus: OnDeviceModelStatus = OnDeviceModelStatus(installed = false),
  val isImportingOnDeviceModel: Boolean = false,
  val onDeviceModelImportProgress: Float? = null,
  val onDeviceModelMessage: String? = null,
  val lastAiProvider: AiExecutionProvider? = null,
  val explanation: String = "",
  val statusMessage: String = "Ready",
  val errorMessage: String? = null,
  val isTestingConnection: Boolean = false,
  val isExplaining: Boolean = false,
  val isImportingScreenshots: Boolean = false,
  val screenshotImportMessage: String? = null,
  val screenshotImportError: Boolean = false,
  val isImportingDocument: Boolean = false,
  val documentImportMessage: String? = null,
  val documentImportError: Boolean = false,
  val studySetTitle: String = "",
  val activeStudySetId: String? = null,
  val studySets: List<StudySet> = emptyList(),
  val isLoadingStudySets: Boolean = true,
  val isSavingStudySet: Boolean = false,
  val hasUnsavedChanges: Boolean = false,
  val libraryMessage: String? = null,
  val flashcards: List<Flashcard> = emptyList(),
  val isGeneratingFlashcards: Boolean = false,
  val tutorMessages: List<TutorMessage> = emptyList(),
  val isAskingTutor: Boolean = false,
)

class MainScreenViewModel(
  computerTutor: StudyTutor,
  private val onDeviceTutor: OnDeviceStudyTutor,
  private val screenshotTextExtractor: ScreenshotTextExtractor,
  private val documentTextExtractor: DocumentTextExtractor,
  private val studySetStore: StudySetStore,
  initialServerUrl: String = DEFAULT_OLLAMA_URL,
  initialAiProviderMode: AiProviderMode = AiProviderMode.Automatic,
  private val saveServerUrl: (String) -> Unit = {},
  private val saveAiProviderMode: (AiProviderMode) -> Unit = {},
) : ViewModel() {
  private val aiTutor = AiTutorCoordinator(computerTutor, onDeviceTutor)
  private val _uiState =
    MutableStateFlow(
      MainScreenUiState(
        serverUrl = initialServerUrl,
        aiProviderMode = initialAiProviderMode,
        onDeviceModelStatus = onDeviceTutor.modelStatus(),
      ),
    )
  val uiState: StateFlow<MainScreenUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      runCatching { studySetStore.loadStudySets() }
        .onSuccess { studySets ->
          _uiState.update { it.copy(studySets = studySets, isLoadingStudySets = false) }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              isLoadingStudySets = false,
              libraryMessage = error.message ?: "Saved study sets could not be loaded.",
            )
          }
        }
    }
  }

  fun updateStudyText(value: String) {
    _uiState.update {
      it.copy(
        studyText = value,
        explanation = "",
        flashcards = emptyList(),
        tutorMessages = emptyList(),
        hasUnsavedChanges = true,
        statusMessage = if (it.explanation.isNotBlank()) "Ready" else it.statusMessage,
        errorMessage = null,
      )
    }
  }

  fun updateStudySetTitle(value: String) {
    _uiState.update { it.copy(studySetTitle = value, hasUnsavedChanges = true, libraryMessage = null) }
  }

  fun newStudySet() {
    _uiState.update {
      it.copy(
        studyText = "",
        explanation = "",
        flashcards = emptyList(),
        tutorMessages = emptyList(),
        studySetTitle = "",
        activeStudySetId = null,
        hasUnsavedChanges = false,
        libraryMessage = "New study set ready",
        errorMessage = null,
        statusMessage = "Ready",
      )
    }
  }

  fun loadStudySet(id: String) {
    val studySet = _uiState.value.studySets.firstOrNull { it.id == id } ?: return
    _uiState.update {
      it.copy(
        studyText = studySet.studyText,
        explanation = studySet.explanation,
        flashcards = studySet.flashcards,
        tutorMessages = studySet.tutorMessages,
        studySetTitle = studySet.title,
        activeStudySetId = studySet.id,
        hasUnsavedChanges = false,
        libraryMessage = "Opened ${studySet.title}",
        errorMessage = null,
        statusMessage = if (studySet.explanation.isBlank()) "Ready" else "Saved explanation loaded",
      )
    }
  }

  fun saveStudySet() {
    val snapshot = _uiState.value
    if (snapshot.studyText.isBlank()) {
      _uiState.update { it.copy(libraryMessage = "Add study text before saving.") }
      return
    }

    val now = System.currentTimeMillis()
    val id = snapshot.activeStudySetId ?: UUID.randomUUID().toString()
    val existing = snapshot.studySets.firstOrNull { it.id == id }
    val title = snapshot.studySetTitle.trim().ifBlank { deriveStudySetTitle(snapshot.studyText) }
    val studySet =
      StudySet(
        id = id,
        title = title,
        studyText = snapshot.studyText,
        explanation = snapshot.explanation,
        flashcards = snapshot.flashcards,
        tutorMessages = snapshot.tutorMessages,
        createdAt = existing?.createdAt ?: now,
        updatedAt = now,
      )

    _uiState.update { it.copy(isSavingStudySet = true, libraryMessage = null) }
    viewModelScope.launch {
      runCatching { studySetStore.saveStudySet(studySet) }
        .onSuccess { studySets ->
          _uiState.update { current ->
            val unchanged =
              current.studyText == snapshot.studyText &&
                current.explanation == snapshot.explanation &&
                current.flashcards == snapshot.flashcards &&
                current.tutorMessages == snapshot.tutorMessages &&
                current.studySetTitle == snapshot.studySetTitle
            current.copy(
              studySets = studySets,
              activeStudySetId = id,
              studySetTitle = if (unchanged) title else current.studySetTitle,
              isSavingStudySet = false,
              hasUnsavedChanges = !unchanged,
              libraryMessage = if (unchanged) "Study set saved" else "Saved; newer changes are not saved yet",
            )
          }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              isSavingStudySet = false,
              libraryMessage = error.message ?: "The study set could not be saved.",
            )
          }
        }
    }
  }

  fun deleteStudySet(id: String) {
    val deletingActiveSet = _uiState.value.activeStudySetId == id
    viewModelScope.launch {
      runCatching { studySetStore.deleteStudySet(id) }
        .onSuccess { studySets ->
          _uiState.update { current ->
            if (deletingActiveSet) {
              current.copy(
                studySets = studySets,
                studyText = "",
                explanation = "",
                flashcards = emptyList(),
                tutorMessages = emptyList(),
                studySetTitle = "",
                activeStudySetId = null,
                hasUnsavedChanges = false,
                libraryMessage = "Study set deleted",
                statusMessage = "Ready",
              )
            } else {
              current.copy(studySets = studySets, libraryMessage = "Study set deleted")
            }
          }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(libraryMessage = error.message ?: "The study set could not be deleted.")
          }
        }
    }
  }

  fun updateServerUrl(value: String) {
    _uiState.update { it.copy(serverUrl = value, statusMessage = "Connection not tested", errorMessage = null) }
  }

  fun updateAiProviderMode(mode: AiProviderMode) {
    saveAiProviderMode(mode)
    _uiState.update { state ->
      state.copy(
        aiProviderMode = mode,
        errorMessage = null,
        statusMessage =
          when (mode) {
            AiProviderMode.Automatic ->
              if (state.onDeviceModelStatus.installed) "Auto - phone AI preferred" else "Auto - computer AI until a phone model is installed"
            AiProviderMode.OnDevice ->
              if (state.onDeviceModelStatus.installed) "Phone AI ready" else "Phone AI model not installed"
            AiProviderMode.Computer -> "Computer AI selected"
          },
      )
    }
  }

  fun importOnDeviceModel(uri: Uri) {
    val state = _uiState.value
    if (state.isImportingOnDeviceModel || state.isExplaining || state.isGeneratingFlashcards || state.isAskingTutor) return
    _uiState.update {
      it.copy(
        isImportingOnDeviceModel = true,
        onDeviceModelImportProgress = null,
        onDeviceModelMessage = "Importing $ON_DEVICE_MODEL_NAME...",
        errorMessage = null,
      )
    }
    viewModelScope.launch {
      runCatching {
          onDeviceTutor.importModel(uri) { progress ->
            _uiState.update { it.copy(onDeviceModelImportProgress = progress?.coerceIn(0f, 1f)) }
          }
        }
        .onSuccess { status ->
          _uiState.update {
            it.copy(
              aiProviderMode = AiProviderMode.OnDevice,
              onDeviceModelStatus = status,
              isImportingOnDeviceModel = false,
              onDeviceModelImportProgress = 1f,
              onDeviceModelMessage = "$ON_DEVICE_MODEL_NAME ready on this phone",
              statusMessage = "Phone AI ready",
            )
          }
          saveAiProviderMode(AiProviderMode.OnDevice)
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              onDeviceModelStatus = onDeviceTutor.modelStatus(),
              isImportingOnDeviceModel = false,
              onDeviceModelImportProgress = null,
              onDeviceModelMessage = "Model import failed",
              errorMessage = error.message ?: "The phone AI model could not be imported.",
            )
          }
        }
    }
  }

  fun removeOnDeviceModel() {
    if (_uiState.value.isImportingOnDeviceModel) return
    viewModelScope.launch {
      runCatching { onDeviceTutor.removeModel() }
        .onSuccess {
          _uiState.update {
            it.copy(
              aiProviderMode = AiProviderMode.Automatic,
              onDeviceModelStatus = onDeviceTutor.modelStatus(),
              onDeviceModelImportProgress = null,
              onDeviceModelMessage = "Phone AI model removed",
              statusMessage = "Auto - using computer AI",
            )
          }
          saveAiProviderMode(AiProviderMode.Automatic)
        }
        .onFailure { error ->
          _uiState.update { it.copy(errorMessage = error.message ?: "The phone AI model could not be removed.") }
        }
    }
  }

  fun importScreenshots(imageUris: List<Uri>, onFinished: () -> Unit = {}) {
    if (imageUris.isEmpty() || _uiState.value.isImportingScreenshots) return

    _uiState.update {
      it.copy(
        isImportingScreenshots = true,
        screenshotImportMessage = "Preparing ${imageUris.size} ${imagesLabel(imageUris.size)}...",
        screenshotImportError = false,
      )
    }
    viewModelScope.launch {
      try {
        val extractedText = mutableListOf<String>()
        var unreadableCount = 0

        imageUris.forEachIndexed { index, imageUri ->
          _uiState.update {
            it.copy(screenshotImportMessage = "Reading image ${index + 1} of ${imageUris.size}...")
          }
          try {
            val text = screenshotTextExtractor.extractText(imageUri)
            if (text.isBlank()) unreadableCount++ else extractedText += text
          } catch (cancellation: CancellationException) {
            throw cancellation
          } catch (_: Exception) {
            unreadableCount++
          }
        }

        _uiState.update { state ->
          if (extractedText.isEmpty()) {
            state.copy(
              isImportingScreenshots = false,
              screenshotImportMessage = "No readable text was found in the selected images.",
              screenshotImportError = true,
            )
          } else {
            val resultMessage =
              if (unreadableCount == 0) {
                "Added text from ${extractedText.size} ${imagesLabel(extractedText.size)}."
              } else {
                "Added text from ${extractedText.size} of ${imageUris.size} images."
              }
            state.copy(
              studyText = mergeStudyText(state.studyText, extractedText),
              explanation = "",
              flashcards = emptyList(),
              tutorMessages = emptyList(),
              hasUnsavedChanges = true,
              isImportingScreenshots = false,
              screenshotImportMessage = resultMessage,
              screenshotImportError = false,
            )
          }
        }
      } finally {
        onFinished()
      }
    }
  }

  fun reportImageImportError(message: String) {
    _uiState.update {
      it.copy(
        isImportingScreenshots = false,
        screenshotImportMessage = message,
        screenshotImportError = true,
      )
    }
  }

  fun importDocument(uri: Uri) {
    val current = _uiState.value
    if (current.isImportingDocument || current.isImportingScreenshots) return

    _uiState.update {
      it.copy(
        isImportingDocument = true,
        documentImportMessage = "Opening document...",
        documentImportError = false,
      )
    }
    viewModelScope.launch {
      runCatching {
          documentTextExtractor.extract(uri) { page, total ->
            _uiState.update {
              it.copy(documentImportMessage = "Reading PDF page $page of $total...")
            }
          }
        }
        .onSuccess { result ->
          _uiState.update { state ->
            if (result.text.isBlank()) {
              state.copy(
                isImportingDocument = false,
                documentImportMessage = "No readable text was found in ${result.fileName}.",
                documentImportError = true,
              )
            } else {
              val suffix = if (result.truncated) " The import limit was reached." else ""
              state.copy(
                studyText = mergeStudyText(state.studyText, listOf(result.text)),
                explanation = "",
                flashcards = emptyList(),
                tutorMessages = emptyList(),
                hasUnsavedChanges = true,
                isImportingDocument = false,
                documentImportMessage = "Added text from ${result.fileName}.$suffix",
                documentImportError = false,
              )
            }
          }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              isImportingDocument = false,
              documentImportMessage = error.message ?: "The document could not be imported.",
              documentImportError = true,
            )
          }
        }
    }
  }

  fun testConnection() {
    val serverUrl = _uiState.value.serverUrl
    _uiState.update { it.copy(isTestingConnection = true, errorMessage = null, statusMessage = "Checking Ollama...") }
    viewModelScope.launch {
      runCatching { aiTutor.checkComputerStatus(serverUrl) }
        .onSuccess { status ->
          saveServerUrl(serverUrl.trim())
          val message = if (status.modelInstalled) "Connected - llama3.2 is ready" else "Connected - llama3.2 is not installed"
          _uiState.update { it.copy(isTestingConnection = false, statusMessage = message) }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              isTestingConnection = false,
              statusMessage = "Connection failed",
              errorMessage = friendlyError(error),
            )
          }
        }
    }
  }

  fun explain() {
    val state = _uiState.value
    if (state.studyText.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Paste some study text first.") }
      return
    }

    _uiState.update {
      it.copy(
        explanation = "",
        isExplaining = true,
        errorMessage = null,
        statusMessage = "Your offline tutor is thinking...",
      )
    }
    viewModelScope.launch {
      runCatching { aiTutor.explain(state.aiProviderMode, state.serverUrl, state.studyText) }
        .onSuccess { result ->
          if (result.provider == AiExecutionProvider.Computer) saveServerUrl(state.serverUrl.trim())
          _uiState.update {
            it.copy(
              explanation = result.value,
              isExplaining = false,
              hasUnsavedChanges = true,
              lastAiProvider = result.provider,
              statusMessage = "Explanation ready on ${result.provider.displayName}",
            )
          }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              isExplaining = false,
              statusMessage = "Could not explain this text",
              errorMessage = friendlyError(error),
            )
          }
        }
    }
  }

  fun generateFlashcards() {
    val state = _uiState.value
    if (state.studyText.isBlank()) {
      _uiState.update { it.copy(errorMessage = "Paste some study text first.") }
      return
    }

    _uiState.update {
      it.copy(
        isGeneratingFlashcards = true,
        errorMessage = null,
        statusMessage = "Creating study cards...",
      )
    }
    viewModelScope.launch {
      runCatching { aiTutor.generateFlashcards(state.aiProviderMode, state.serverUrl, state.studyText) }
        .onSuccess { result ->
          if (result.provider == AiExecutionProvider.Computer) saveServerUrl(state.serverUrl.trim())
          _uiState.update {
            it.copy(
              flashcards = result.value,
              isGeneratingFlashcards = false,
              hasUnsavedChanges = true,
              lastAiProvider = result.provider,
              statusMessage = "${result.value.size} study cards ready on ${result.provider.displayName}",
            )
          }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              isGeneratingFlashcards = false,
              statusMessage = "Could not create study cards",
              errorMessage = friendlyError(error),
            )
          }
        }
    }
  }

  fun askTutor(question: String) {
    val state = _uiState.value
    val cleanQuestion = question.trim()
    when {
      state.studyText.isBlank() -> {
        _uiState.update { it.copy(errorMessage = "Add some study text before asking the tutor.") }
        return
      }
      cleanQuestion.isBlank() || state.isAskingTutor -> return
    }

    val studentMessage = TutorMessage(role = TutorMessageRole.Student, text = cleanQuestion)
    _uiState.update {
      it.copy(
        tutorMessages = it.tutorMessages + studentMessage,
        isAskingTutor = true,
        errorMessage = null,
        hasUnsavedChanges = true,
        statusMessage = "Your tutor is thinking...",
      )
    }
    viewModelScope.launch {
      runCatching {
          aiTutor.askQuestion(
            mode = state.aiProviderMode,
            serverUrl = state.serverUrl,
            studyText = state.studyText,
            history = state.tutorMessages,
            question = cleanQuestion,
          )
        }
        .onSuccess { result ->
          if (result.provider == AiExecutionProvider.Computer) saveServerUrl(state.serverUrl.trim())
          _uiState.update {
            it.copy(
              tutorMessages = it.tutorMessages + TutorMessage(role = TutorMessageRole.Tutor, text = result.value),
              isAskingTutor = false,
              hasUnsavedChanges = true,
              lastAiProvider = result.provider,
              statusMessage = "Tutor answered on ${result.provider.displayName}",
            )
          }
        }
        .onFailure { error ->
          _uiState.update {
            it.copy(
              isAskingTutor = false,
              statusMessage = "The tutor could not answer",
              errorMessage = friendlyError(error),
            )
          }
        }
    }
  }

  fun updateFlashcard(id: String, question: String, answer: String) {
    if (question.isBlank() || answer.isBlank()) return
    _uiState.update { state ->
      state.copy(
        flashcards =
          state.flashcards.map { card ->
            if (card.id == id) card.copy(question = question.trim(), answer = answer.trim()) else card
          },
        hasUnsavedChanges = true,
      )
    }
  }

  fun deleteFlashcard(id: String) {
    _uiState.update {
      it.copy(
        flashcards = it.flashcards.filterNot { card -> card.id == id },
        hasUnsavedChanges = true,
      )
    }
  }

  override fun onCleared() {
    screenshotTextExtractor.close()
    documentTextExtractor.close()
    onDeviceTutor.close()
  }
}

internal fun mergeStudyText(existingText: String, importedText: List<String>): String =
  (listOf(existingText.trim()) + importedText.map(String::trim))
    .filter(String::isNotBlank)
    .joinToString("\n\n")

private fun imagesLabel(count: Int): String = if (count == 1) "image" else "images"

private fun friendlyError(error: Throwable): String =
  when (error) {
    is OnDeviceModelNotInstalledException -> error.message ?: "Install the phone AI model in Settings."
    is AiProvidersUnavailableException -> error.message ?: "Neither phone AI nor computer AI is available."
    is ConnectException -> "Could not reach Ollama. Check the address, Wi-Fi, and Ollama on your computer."
    is UnknownHostException -> "That computer address could not be found. Check the IP address."
    is SocketTimeoutException -> "Ollama took too long to respond. Try a shorter passage."
    is IllegalArgumentException -> error.message ?: "Check the Ollama address."
    else -> error.message ?: "Something went wrong while running the AI tutor."
  }
