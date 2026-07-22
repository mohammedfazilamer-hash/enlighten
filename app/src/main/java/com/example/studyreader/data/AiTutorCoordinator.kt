package com.example.studyreader.data

import kotlinx.coroutines.CancellationException

enum class AiProviderMode(val displayName: String) {
  Automatic("Auto"),
  OnDevice("Phone"),
  Computer("Computer"),
}

enum class AiExecutionProvider(val displayName: String) {
  OnDevice("this phone"),
  Computer("your computer"),
}

data class AiResult<T>(val value: T, val provider: AiExecutionProvider)

class AiProvidersUnavailableException(phoneError: Throwable, computerError: Throwable) :
  IllegalStateException(
    "Phone AI failed: ${phoneError.message ?: "unknown error"}. " +
      "Computer fallback failed: ${computerError.message ?: "unknown error"}.",
    computerError,
  )

class AiTutorCoordinator(
  private val computerTutor: StudyTutor,
  private val onDeviceTutor: OnDeviceStudyTutor,
) {
  suspend fun checkComputerStatus(serverUrl: String): OllamaStatus = computerTutor.checkStatus(serverUrl)

  suspend fun explain(mode: AiProviderMode, serverUrl: String, studyText: String): AiResult<String> =
    execute(
      mode = mode,
      onDeviceRequest = { onDeviceTutor.explain(serverUrl, studyText) },
      computerRequest = { computerTutor.explain(serverUrl, studyText) },
    )

  suspend fun generateFlashcards(
    mode: AiProviderMode,
    serverUrl: String,
    studyText: String,
  ): AiResult<List<Flashcard>> =
    execute(
      mode = mode,
      onDeviceRequest = { onDeviceTutor.generateFlashcards(serverUrl, studyText) },
      computerRequest = { computerTutor.generateFlashcards(serverUrl, studyText) },
    )

  suspend fun askQuestion(
    mode: AiProviderMode,
    serverUrl: String,
    studyText: String,
    history: List<TutorMessage>,
    question: String,
  ): AiResult<String> =
    execute(
      mode = mode,
      onDeviceRequest = { onDeviceTutor.askQuestion(serverUrl, studyText, history, question) },
      computerRequest = { computerTutor.askQuestion(serverUrl, studyText, history, question) },
    )

  private suspend fun <T> execute(
    mode: AiProviderMode,
    onDeviceRequest: suspend () -> T,
    computerRequest: suspend () -> T,
  ): AiResult<T> {
    val providers = providerOrder(mode, onDeviceTutor.modelStatus().installed)
    var phoneError: Throwable? = null
    for (provider in providers) {
      try {
        return when (provider) {
          AiExecutionProvider.OnDevice -> AiResult(onDeviceRequest(), provider)
          AiExecutionProvider.Computer -> AiResult(computerRequest(), provider)
        }
      } catch (cancellation: CancellationException) {
        throw cancellation
      } catch (error: Throwable) {
        if (provider == AiExecutionProvider.OnDevice && providers.size > 1) {
          phoneError = error
        } else if (phoneError != null) {
          throw AiProvidersUnavailableException(phoneError, error)
        } else {
          throw error
        }
      }
    }
    throw IllegalStateException("No AI provider is available.")
  }
}

internal fun providerOrder(
  mode: AiProviderMode,
  onDeviceModelInstalled: Boolean,
): List<AiExecutionProvider> =
  when (mode) {
    AiProviderMode.Automatic ->
      if (onDeviceModelInstalled) {
        listOf(AiExecutionProvider.OnDevice, AiExecutionProvider.Computer)
      } else {
        listOf(AiExecutionProvider.Computer)
      }
    AiProviderMode.OnDevice -> listOf(AiExecutionProvider.OnDevice)
    AiProviderMode.Computer -> listOf(AiExecutionProvider.Computer)
  }
