package com.example.studyreader.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts.PickMultipleVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia
import androidx.activity.result.contract.ActivityResultContracts.TakePicture
import androidx.activity.result.contract.ActivityResultContracts.OpenDocument
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Save
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.studyreader.data.DEFAULT_OLLAMA_URL
import com.example.studyreader.data.AndroidDocumentTextExtractor
import com.example.studyreader.data.FileStudySetStore
import com.example.studyreader.data.MlKitScreenshotTextExtractor
import com.example.studyreader.data.OllamaClient
import com.example.studyreader.data.ProfileImageStore
import com.example.studyreader.data.createCameraImageUri
import com.example.studyreader.data.deleteCameraImage
import com.example.studyreader.theme.StudyPalette
import com.example.studyreader.theme.StudyReaderTheme
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.launch

private const val PREFERENCES_NAME = "study_reader"
private const val SERVER_URL_KEY = "ollama_server_url"
private const val AUTO_READ_EXPLANATION_KEY = "auto_read_explanation"
private const val VOICE_NAME_KEY = "tts_voice_name"
private const val PROFILE_IMAGE_URI_KEY = "profile_image_uri"
private const val VOICE_PREVIEW_UTTERANCE_ID = "study-reader-voice-preview"

private enum class NarrationPhase {
  Idle,
  ReadingStudyText,
  WaitingForExplanation,
  ReadingExplanation,
}

private enum class SpeechContent {
  StudyText,
  Explanation,
}

private data class QueuedSpeechRange(
  val content: SpeechContent,
  val startOffset: Int,
  val endOffset: Int,
  val segmentIndex: Int,
  val generation: Int,
)

private data class ActiveSpeechHighlight(
  val content: SpeechContent,
  val range: TextHighlightRange,
)

internal data class TextHighlightRange(
  val start: Int,
  val endExclusive: Int,
)

private enum class AppSection {
  Study,
  Library,
  Settings,
}

@Composable
fun MainScreen(
  selectedPalette: StudyPalette,
  onPaletteSelected: (StudyPalette) -> Unit,
  modifier: Modifier = Modifier,
) {
  val context = LocalContext.current
  val preferences = remember(context) { context.getSharedPreferences(PREFERENCES_NAME, 0) }
  val screenViewModel: MainScreenViewModel =
    viewModel {
      MainScreenViewModel(
        studyTutor = OllamaClient(),
        screenshotTextExtractor = MlKitScreenshotTextExtractor(context.applicationContext),
        documentTextExtractor = AndroidDocumentTextExtractor(context.applicationContext),
        studySetStore = FileStudySetStore(context.applicationContext),
        initialServerUrl = preferences.getString(SERVER_URL_KEY, DEFAULT_OLLAMA_URL) ?: DEFAULT_OLLAMA_URL,
        saveServerUrl = { preferences.edit().putString(SERVER_URL_KEY, it).apply() },
      )
    }
  val state by screenViewModel.uiState.collectAsStateWithLifecycle()
  val screenshotPicker =
    rememberLauncherForActivityResult(PickMultipleVisualMedia(maxItems = 10)) { imageUris ->
      screenViewModel.importScreenshots(imageUris)
    }
  val documentPicker =
    rememberLauncherForActivityResult(OpenDocument()) { documentUri ->
      documentUri?.let(screenViewModel::importDocument)
    }
  var pendingCameraUri by rememberSaveable { mutableStateOf<String?>(null) }
  val cameraLauncher =
    rememberLauncherForActivityResult(TakePicture()) { photoSaved ->
      val imageUri = pendingCameraUri?.let(Uri::parse)
      pendingCameraUri = null
      if (imageUri != null) {
        if (photoSaved) {
          screenViewModel.importScreenshots(listOf(imageUri)) {
            deleteCameraImage(context, imageUri)
          }
        } else {
          deleteCameraImage(context, imageUri)
        }
      }
    }
  val uiScope = rememberCoroutineScope()
  val profileImageStore = remember(context) { ProfileImageStore(context.applicationContext) }
  var profileImageVersion by remember { mutableIntStateOf(0) }
  var profileImageError by remember { mutableStateOf<String?>(null) }
  val profileImage: ImageBitmap? =
    remember(profileImageVersion) { profileImageStore.load()?.asImageBitmap() }
  val profileImagePicker =
    rememberLauncherForActivityResult(PickVisualMedia()) { imageUri ->
      if (imageUri != null) {
        runCatching {
          context.contentResolver.takePersistableUriPermission(imageUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        preferences.edit().putString(PROFILE_IMAGE_URI_KEY, imageUri.toString()).commit()
        uiScope.launch {
          runCatching { profileImageStore.save(context.applicationContext, imageUri) }
            .onSuccess {
              profileImageVersion++
              profileImageError = null
            }
            .onFailure {
              profileImageError = "The selected profile photo could not be saved."
            }
        }
      }
    }

  LaunchedEffect(profileImageStore) {
    val savedProfileUri = preferences.getString(PROFILE_IMAGE_URI_KEY, null)?.let(Uri::parse)
    if (!profileImageStore.exists() && savedProfileUri != null) {
      runCatching { profileImageStore.save(context.applicationContext, savedProfileUri) }
        .onSuccess {
          profileImageVersion++
          profileImageError = null
        }
        .onFailure {
          profileImageError = "Choose your profile photo again to restore it."
        }
    }
  }

  var speechEngine by remember { mutableStateOf<TextToSpeech?>(null) }
  var speechReady by remember { mutableStateOf(false) }
  var speechRate by remember { mutableFloatStateOf(1f) }
  var speechMessage by remember { mutableStateOf("Preparing phone voice...") }
  var narrationPhase by remember { mutableStateOf(NarrationPhase.Idle) }
  var autoReadRequested by remember { mutableStateOf(false) }
  var autoReadExplanation by remember {
    mutableStateOf(preferences.getBoolean(AUTO_READ_EXPLANATION_KEY, true))
  }
  var voiceOptions by remember { mutableStateOf(emptyList<VoiceOption>()) }
  var selectedVoiceName by remember {
    mutableStateOf(preferences.getString(VOICE_NAME_KEY, null))
  }
  val mainHandler = remember { Handler(Looper.getMainLooper()) }
  val queuedSpeechRanges = remember { ConcurrentHashMap<String, QueuedSpeechRange>() }
  var speechHighlight by remember { mutableStateOf<ActiveSpeechHighlight?>(null) }
  var speechSegments by remember { mutableStateOf(emptyList<SpeechChunk>()) }
  var currentSpeechSegment by remember { mutableIntStateOf(0) }
  var isSpeechPaused by remember { mutableStateOf(false) }
  var speechPlaybackGeneration by remember { mutableIntStateOf(0) }

  DisposableEffect(context) {
    lateinit var engine: TextToSpeech
    fun reportSpeechError() {
      mainHandler.post {
        narrationPhase = NarrationPhase.Idle
        autoReadRequested = false
        isSpeechPaused = false
        speechSegments = emptyList()
        speechHighlight = null
        queuedSpeechRanges.clear()
        speechMessage = "Reading stopped because the phone voice reported an error"
      }
    }
    engine =
      TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
          val languageResult = engine.setLanguage(Locale.getDefault())
          speechReady = languageResult != TextToSpeech.LANG_MISSING_DATA && languageResult != TextToSpeech.LANG_NOT_SUPPORTED
          speechMessage = if (speechReady) "Phone voice ready" else "Your phone voice does not support this language"
          val installedOfflineVoices =
            engine.voices.orEmpty()
              .filterNot(Voice::isNetworkConnectionRequired)
              .sortedWith(compareBy({ it.locale.getDisplayName(Locale.getDefault()) }, Voice::getName))
          val preferredVoice =
            installedOfflineVoices.firstOrNull { it.name == selectedVoiceName }
              ?: installedOfflineVoices.firstOrNull { it.name == engine.voice?.name }
              ?: installedOfflineVoices.firstOrNull { it.locale == Locale.getDefault() }
              ?: installedOfflineVoices.firstOrNull()
          if (preferredVoice != null) {
            engine.setVoice(preferredVoice)
            selectedVoiceName = preferredVoice.name
          }
          voiceOptions = installedOfflineVoices.map(Voice::toVoiceOption)
        } else {
          speechMessage = "Phone voice could not start"
        }
      }
    engine.setOnUtteranceProgressListener(
      object : UtteranceProgressListener() {
        override fun onStart(utteranceId: String?) {
          val queuedRange = utteranceId?.let(queuedSpeechRanges::get) ?: return
          mainHandler.post {
            speechHighlight =
              ActiveSpeechHighlight(
                content = queuedRange.content,
                range = TextHighlightRange(queuedRange.startOffset, queuedRange.endOffset),
              )
          }
        }

        override fun onRangeStart(utteranceId: String?, start: Int, end: Int, frame: Int) {
          val queuedRange = utteranceId?.let(queuedSpeechRanges::get) ?: return
          mainHandler.post {
            speechHighlight =
              ActiveSpeechHighlight(
                content = queuedRange.content,
                range =
                  TextHighlightRange(
                    start = (queuedRange.startOffset + start).coerceAtMost(queuedRange.endOffset),
                    endExclusive = (queuedRange.startOffset + end).coerceAtMost(queuedRange.endOffset),
                  ),
              )
          }
        }

        override fun onDone(utteranceId: String?) {
          if (utteranceId == VOICE_PREVIEW_UTTERANCE_ID) {
            mainHandler.post { speechMessage = "Voice preview finished" }
            return
          }
          val completedRange = utteranceId?.let(queuedSpeechRanges::remove) ?: return
          mainHandler.post {
            if (completedRange.generation != speechPlaybackGeneration || isSpeechPaused) return@post
            if (completedRange.segmentIndex < speechSegments.lastIndex) {
              currentSpeechSegment = completedRange.segmentIndex + 1
            } else {
              when (completedRange.content) {
                SpeechContent.StudyText -> {
                  speechHighlight = null
                  if (autoReadRequested) {
                    narrationPhase = NarrationPhase.WaitingForExplanation
                    speechSegments = emptyList()
                    currentSpeechSegment = 0
                    speechMessage = "Study text finished. Preparing AI explanation..."
                  } else {
                    narrationPhase = NarrationPhase.Idle
                    speechSegments = emptyList()
                    speechMessage = "Finished reading"
                  }
                }

                SpeechContent.Explanation -> {
                narrationPhase = NarrationPhase.Idle
                autoReadRequested = false
                isSpeechPaused = false
                speechSegments = emptyList()
                speechHighlight = null
                queuedSpeechRanges.clear()
                speechMessage = "Finished reading study text and AI explanation"
                }
              }
            }
          }
        }

        @Deprecated("Deprecated in Java")
        override fun onError(utteranceId: String?) {
          reportSpeechError()
        }

        override fun onError(utteranceId: String?, errorCode: Int) {
          reportSpeechError()
        }
      },
    )
    speechEngine = engine

    onDispose {
      engine.stop()
      engine.shutdown()
      queuedSpeechRanges.clear()
      speechEngine = null
    }
  }

  LaunchedEffect(
    narrationPhase,
    state.explanation,
    state.isExplaining,
    state.errorMessage,
    speechReady,
  ) {
    if (narrationPhase != NarrationPhase.WaitingForExplanation) return@LaunchedEffect

    val explanationSegments = splitTextForSpeechSegments(state.explanation)
    if (explanationSegments.isNotEmpty() && speechEngine != null && speechReady) {
      narrationPhase = NarrationPhase.ReadingExplanation
      speechSegments = explanationSegments
      currentSpeechSegment = 0
      isSpeechPaused = false
      speechPlaybackGeneration++
      speechMessage = "Reading AI explanation"
    } else if (!state.isExplaining) {
      narrationPhase = NarrationPhase.Idle
      autoReadRequested = false
      speechMessage = "Study text finished. AI explanation could not be read."
    }
  }

  LaunchedEffect(
    narrationPhase,
    currentSpeechSegment,
    isSpeechPaused,
    speechPlaybackGeneration,
    speechReady,
  ) {
    val content =
      when (narrationPhase) {
        NarrationPhase.ReadingStudyText -> SpeechContent.StudyText
        NarrationPhase.ReadingExplanation -> SpeechContent.Explanation
        else -> return@LaunchedEffect
      }
    if (isSpeechPaused || !speechReady) return@LaunchedEffect
    val segment = speechSegments.getOrNull(currentSpeechSegment) ?: return@LaunchedEffect
    val engine = speechEngine ?: return@LaunchedEffect
    engine.stop()
    queuedSpeechRanges.clear()
    engine.setSpeechRate(speechRate)
    val utteranceId = "study-reader-${content.name}-$speechPlaybackGeneration-$currentSpeechSegment"
    queuedSpeechRanges[utteranceId] =
      QueuedSpeechRange(
        content = content,
        startOffset = segment.startOffset,
        endOffset = segment.endOffset,
        segmentIndex = currentSpeechSegment,
        generation = speechPlaybackGeneration,
      )
    engine.speak(segment.text, TextToSpeech.QUEUE_FLUSH, null, utteranceId)
  }

  StudyReaderScreen(
    state = state,
    speechRate = speechRate,
    speechReady = speechReady,
    isNarrationActive = narrationPhase != NarrationPhase.Idle,
    playbackControlsEnabled =
      narrationPhase == NarrationPhase.ReadingStudyText || narrationPhase == NarrationPhase.ReadingExplanation,
    isSpeechPaused = isSpeechPaused,
    playbackProgress =
      if (speechSegments.isEmpty()) 0f else (currentSpeechSegment + 1f) / speechSegments.size,
    playbackPositionLabel =
      if (speechSegments.isEmpty()) "" else "Sentence ${currentSpeechSegment + 1} of ${speechSegments.size}",
    canSkipPrevious = speechSegments.isNotEmpty() && currentSpeechSegment > 0,
    canSkipNext = speechSegments.isNotEmpty() && currentSpeechSegment < speechSegments.lastIndex,
    readButtonLabel =
      when (narrationPhase) {
        NarrationPhase.Idle -> "Read aloud"
        NarrationPhase.ReadingStudyText -> "Reading text"
        NarrationPhase.WaitingForExplanation -> "Waiting for AI"
        NarrationPhase.ReadingExplanation -> "Reading AI"
      },
    speechMessage = speechMessage,
    studyTextHighlight = speechHighlight?.takeIf { it.content == SpeechContent.StudyText }?.range,
    explanationHighlight = speechHighlight?.takeIf { it.content == SpeechContent.Explanation }?.range,
    autoReadExplanation = autoReadExplanation,
    selectedPalette = selectedPalette,
    onPaletteSelected = onPaletteSelected,
    profileImage = profileImage,
    profileImageError = profileImageError,
    onProfileImageClick = {
      profileImagePicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    },
    voiceOptions = voiceOptions,
    selectedVoiceName = selectedVoiceName,
    onVoiceSelected = { voiceName ->
      val engine = speechEngine
      val voice = engine?.voices?.firstOrNull { it.name == voiceName && !it.isNetworkConnectionRequired }
      if (engine != null && voice != null && engine.setVoice(voice) == TextToSpeech.SUCCESS) {
        selectedVoiceName = voice.name
        preferences.edit().putString(VOICE_NAME_KEY, voice.name).apply()
        speechMessage = "Reading voice changed"
      }
    },
    onPreviewVoice = {
      speechEngine?.let { engine ->
        engine.stop()
        engine.setSpeechRate(speechRate)
        engine.speak(
          "This is your selected Enlighten voice.",
          TextToSpeech.QUEUE_FLUSH,
          null,
          VOICE_PREVIEW_UTTERANCE_ID,
        )
        speechMessage = "Playing voice preview"
      }
    },
    onStudyTextChange = screenViewModel::updateStudyText,
    onStudySetTitleChange = screenViewModel::updateStudySetTitle,
    onSaveStudySet = screenViewModel::saveStudySet,
    onNewStudySet = {
      speechEngine?.stop()
      narrationPhase = NarrationPhase.Idle
      autoReadRequested = false
      isSpeechPaused = false
      speechSegments = emptyList()
      speechHighlight = null
      queuedSpeechRanges.clear()
      screenViewModel.newStudySet()
    },
    onOpenStudySet = { id ->
      speechEngine?.stop()
      narrationPhase = NarrationPhase.Idle
      autoReadRequested = false
      isSpeechPaused = false
      speechSegments = emptyList()
      speechHighlight = null
      queuedSpeechRanges.clear()
      screenViewModel.loadStudySet(id)
    },
    onDeleteStudySet = screenViewModel::deleteStudySet,
    onAddScreenshots = {
      screenshotPicker.launch(PickVisualMediaRequest(PickVisualMedia.ImageOnly))
    },
    onImportDocument = {
      documentPicker.launch(
        arrayOf(
          "application/pdf",
          "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
          "text/plain",
        ),
      )
    },
    onTakePhoto = {
      runCatching { createCameraImageUri(context) }
        .onSuccess { imageUri ->
          pendingCameraUri = imageUri.toString()
          runCatching { cameraLauncher.launch(imageUri) }
            .onFailure {
              pendingCameraUri = null
              deleteCameraImage(context, imageUri)
              screenViewModel.reportImageImportError("The camera could not be opened.")
            }
        }
        .onFailure {
          screenViewModel.reportImageImportError("The camera image could not be prepared.")
        }
    },
    onServerUrlChange = screenViewModel::updateServerUrl,
    onSpeechRateChange = { speechRate = it },
    onAutoReadExplanationChange = { enabled ->
      autoReadExplanation = enabled
      preferences.edit().putBoolean(AUTO_READ_EXPLANATION_KEY, enabled).apply()
    },
    onRead = {
      val segments = splitTextForSpeechSegments(state.studyText)
      val engine = speechEngine
      if (engine != null && speechReady && segments.isNotEmpty()) {
        engine.stop()
        queuedSpeechRanges.clear()
        speechHighlight = null
        autoReadRequested = autoReadExplanation
        narrationPhase = NarrationPhase.ReadingStudyText
        speechSegments = segments
        currentSpeechSegment = 0
        isSpeechPaused = false
        speechPlaybackGeneration++
        if (autoReadExplanation && state.explanation.isBlank() && !state.isExplaining) {
          screenViewModel.explain()
        }
        speechMessage =
          if (autoReadExplanation) {
            "Reading study text and preparing AI explanation"
          } else {
            "Reading study text"
          }
      } else if (state.studyText.isBlank()) {
        speechMessage = "Paste some study text first"
      }
    },
    onStop = {
      speechEngine?.stop()
      narrationPhase = NarrationPhase.Idle
      autoReadRequested = false
      isSpeechPaused = false
      speechSegments = emptyList()
      speechHighlight = null
      queuedSpeechRanges.clear()
      speechMessage = "Reading stopped"
    },
    onPauseResume = {
      if (narrationPhase == NarrationPhase.ReadingStudyText || narrationPhase == NarrationPhase.ReadingExplanation) {
        if (isSpeechPaused) {
          isSpeechPaused = false
          speechPlaybackGeneration++
          speechMessage = "Reading resumed"
        } else {
          speechEngine?.stop()
          queuedSpeechRanges.clear()
          isSpeechPaused = true
          speechMessage = "Reading paused"
        }
      }
    },
    onPreviousSegment = {
      if (speechSegments.isNotEmpty()) {
        speechEngine?.stop()
        queuedSpeechRanges.clear()
        currentSpeechSegment = (currentSpeechSegment - 1).coerceAtLeast(0)
        speechHighlight = null
        speechPlaybackGeneration++
      }
    },
    onNextSegment = {
      if (speechSegments.isNotEmpty()) {
        speechEngine?.stop()
        queuedSpeechRanges.clear()
        currentSpeechSegment = (currentSpeechSegment + 1).coerceAtMost(speechSegments.lastIndex)
        speechHighlight = null
        speechPlaybackGeneration++
      }
    },
    onTestConnection = screenViewModel::testConnection,
    onExplain = screenViewModel::explain,
    onGenerateFlashcards = screenViewModel::generateFlashcards,
    onAskTutor = screenViewModel::askTutor,
    onUpdateFlashcard = screenViewModel::updateFlashcard,
    onDeleteFlashcard = screenViewModel::deleteFlashcard,
    modifier = modifier,
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun StudyReaderScreen(
  state: MainScreenUiState,
  speechRate: Float,
  speechReady: Boolean,
  isNarrationActive: Boolean,
  playbackControlsEnabled: Boolean,
  isSpeechPaused: Boolean,
  playbackProgress: Float,
  playbackPositionLabel: String,
  canSkipPrevious: Boolean,
  canSkipNext: Boolean,
  readButtonLabel: String,
  speechMessage: String,
  studyTextHighlight: TextHighlightRange?,
  explanationHighlight: TextHighlightRange?,
  autoReadExplanation: Boolean,
  selectedPalette: StudyPalette,
  onPaletteSelected: (StudyPalette) -> Unit,
  profileImage: ImageBitmap?,
  profileImageError: String?,
  onProfileImageClick: () -> Unit,
  voiceOptions: List<VoiceOption>,
  selectedVoiceName: String?,
  onVoiceSelected: (String) -> Unit,
  onPreviewVoice: () -> Unit,
  onStudyTextChange: (String) -> Unit,
  onStudySetTitleChange: (String) -> Unit,
  onSaveStudySet: () -> Unit,
  onNewStudySet: () -> Unit,
  onOpenStudySet: (String) -> Unit,
  onDeleteStudySet: (String) -> Unit,
  onAddScreenshots: () -> Unit,
  onImportDocument: () -> Unit,
  onTakePhoto: () -> Unit,
  onServerUrlChange: (String) -> Unit,
  onSpeechRateChange: (Float) -> Unit,
  onAutoReadExplanationChange: (Boolean) -> Unit,
  onRead: () -> Unit,
  onStop: () -> Unit,
  onPauseResume: () -> Unit,
  onPreviousSegment: () -> Unit,
  onNextSegment: () -> Unit,
  onTestConnection: () -> Unit,
  onExplain: () -> Unit,
  onGenerateFlashcards: () -> Unit,
  onAskTutor: (String) -> Unit,
  onUpdateFlashcard: (String, String, String) -> Unit,
  onDeleteFlashcard: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusManager = LocalFocusManager.current
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val drawerScope = rememberCoroutineScope()
  var selectedSectionName by rememberSaveable { mutableStateOf(AppSection.Study.name) }
  val selectedSection = AppSection.entries.firstOrNull { it.name == selectedSectionName } ?: AppSection.Study
  val profileControlsEnabled = !isNarrationActive && !state.isImportingScreenshots && !state.isImportingDocument

  fun selectSection(section: AppSection) {
    selectedSectionName = section.name
    drawerScope.launch { drawerState.close() }
  }

  ModalNavigationDrawer(
    drawerState = drawerState,
    drawerContent = {
      ModalDrawerSheet(modifier = Modifier.width(300.dp)) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 24.dp),
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          Row(
            modifier =
              Modifier.fillMaxWidth()
                .clickable(enabled = profileControlsEnabled, onClick = onProfileImageClick)
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            ProfileAvatar(profileImage = profileImage, size = 56.dp)
            Column {
              Text("Profile", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
              Text("Enlighten", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          profileImageError?.let { message ->
            Text(message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
          }
          HorizontalDivider()
          NavigationDrawerItem(
            label = { Text("Study") },
            selected = selectedSection == AppSection.Study,
            onClick = { selectSection(AppSection.Study) },
            icon = { Icon(Icons.Rounded.School, contentDescription = null) },
          )
          NavigationDrawerItem(
            label = { Text("Library") },
            selected = selectedSection == AppSection.Library,
            onClick = { if (!isNarrationActive) selectSection(AppSection.Library) },
            icon = { Icon(Icons.Rounded.CollectionsBookmark, contentDescription = null) },
            badge = { if (state.studySets.isNotEmpty()) Text(state.studySets.size.toString()) },
          )
          NavigationDrawerItem(
            label = { Text("Settings") },
            selected = selectedSection == AppSection.Settings,
            onClick = { if (!isNarrationActive) selectSection(AppSection.Settings) },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
          )
        }
      }
    },
  ) {
    Scaffold(
      modifier = modifier.fillMaxSize(),
      topBar = {
        TopAppBar(
          navigationIcon = {
            Row(modifier = Modifier.width(96.dp), verticalAlignment = Alignment.CenterVertically) {
              IconButton(onClick = { drawerScope.launch { drawerState.open() } }) {
                Icon(Icons.Rounded.Menu, contentDescription = "Open menu")
              }
              IconButton(onClick = onProfileImageClick, enabled = profileControlsEnabled) {
                ProfileAvatar(profileImage = profileImage, size = 32.dp)
              }
            }
          },
          title = {
            when (selectedSection) {
              AppSection.Study -> {
                Column {
                  Text("Enlighten", style = MaterialTheme.typography.titleLarge)
                  Text("Offline tutor", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
              }
              AppSection.Library -> Text("Library", style = MaterialTheme.typography.titleLarge)
              AppSection.Settings -> Text("Settings", style = MaterialTheme.typography.titleLarge)
            }
          },
        )
      },
    ) { innerPadding ->
      when (selectedSection) {
        AppSection.Settings -> {
          SettingsContent(
            selectedPalette = selectedPalette,
            onPaletteSelected = onPaletteSelected,
            voiceOptions = voiceOptions,
            selectedVoiceName = selectedVoiceName,
            onVoiceSelected = onVoiceSelected,
            onPreviewVoice = onPreviewVoice,
            voiceControlsEnabled = speechReady && !isNarrationActive,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
          )
        }

        AppSection.Library -> {
          StudySetLibrary(
            studySets = state.studySets,
            activeStudySetId = state.activeStudySetId,
            hasUnsavedChanges = state.hasUnsavedChanges,
            isLoading = state.isLoadingStudySets,
            message = state.libraryMessage,
            onNewStudySet = {
              onNewStudySet()
              selectedSectionName = AppSection.Study.name
            },
            onOpenStudySet = { id ->
              onOpenStudySet(id)
              selectedSectionName = AppSection.Study.name
            },
            onDeleteStudySet = onDeleteStudySet,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
          )
        }

        AppSection.Study -> {
        Column(
          modifier =
            Modifier.fillMaxSize()
              .padding(innerPadding)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 16.dp, vertical = 12.dp),
          verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = state.studySetTitle,
          onValueChange = onStudySetTitleChange,
          enabled = !isNarrationActive && !state.isSavingStudySet,
          label = { Text("Study set name") },
          placeholder = { Text("A title will be created when you save") },
          supportingText = {
            Text(
              when {
                state.isSavingStudySet -> "Saving on this phone..."
                state.hasUnsavedChanges -> "Unsaved changes"
                state.activeStudySetId != null -> "Saved on this phone"
                else -> "New study set"
              },
            )
          },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
          Button(
            onClick = {
              focusManager.clearFocus()
              onSaveStudySet()
            },
            enabled =
              state.studyText.isNotBlank() &&
                !state.isSavingStudySet &&
                !state.isImportingScreenshots &&
                !state.isImportingDocument &&
                !state.isExplaining &&
                !state.isGeneratingFlashcards &&
                !state.isAskingTutor &&
                !isNarrationActive,
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            if (state.isSavingStudySet) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            } else {
              Icon(Icons.Rounded.Save, contentDescription = null)
            }
            Spacer(Modifier.size(8.dp))
            Text(if (state.activeStudySetId == null) "Save set" else "Save changes")
          }
          OutlinedButton(
            onClick = {
              focusManager.clearFocus()
              selectedSectionName = AppSection.Library.name
            },
            enabled = !isNarrationActive,
            modifier = Modifier.weight(1f).height(48.dp).testTag("open-library"),
          ) {
            Icon(Icons.Rounded.CollectionsBookmark, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Library")
          }
        }
        state.libraryMessage?.let { message ->
          Text(message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Study text", style = MaterialTheme.typography.titleLarge)
        Text(
          "Paste text or import images and documents.",
          style = MaterialTheme.typography.bodyLarge,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          OutlinedButton(
            onClick = {
              focusManager.clearFocus()
              onAddScreenshots()
            },
            enabled = !state.isImportingScreenshots && !state.isImportingDocument && !isNarrationActive,
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Icon(Icons.Rounded.AddPhotoAlternate, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Screenshots")
          }
          OutlinedButton(
            onClick = {
              focusManager.clearFocus()
              onTakePhoto()
            },
            enabled = !state.isImportingScreenshots && !state.isImportingDocument && !isNarrationActive,
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Icon(Icons.Rounded.CameraAlt, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Camera")
          }
        }
        OutlinedButton(
          onClick = {
            focusManager.clearFocus()
            onImportDocument()
          },
          enabled = !state.isImportingScreenshots && !state.isImportingDocument && !isNarrationActive,
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          Icon(Icons.Rounded.Description, contentDescription = null)
          Spacer(Modifier.size(8.dp))
          Text("PDF or document")
        }
        state.screenshotImportMessage?.let { message ->
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.isImportingScreenshots) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            Text(
              message,
              style = MaterialTheme.typography.labelSmall,
              color =
                if (state.screenshotImportError) {
                  MaterialTheme.colorScheme.error
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
          }
        }
        state.documentImportMessage?.let { message ->
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (state.isImportingDocument) {
              CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            }
            Text(
              message,
              style = MaterialTheme.typography.labelSmall,
              color =
                if (state.documentImportError) {
                  MaterialTheme.colorScheme.error
                } else {
                  MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
          }
        }
        OutlinedTextField(
          value = state.studyText,
          onValueChange = onStudyTextChange,
          enabled = !state.isImportingScreenshots && !state.isImportingDocument,
          readOnly = isNarrationActive || state.isExplaining || state.isGeneratingFlashcards || state.isAskingTutor,
          visualTransformation = HighlightVisualTransformation(studyTextHighlight, MaterialTheme.colorScheme.primaryContainer),
          modifier = Modifier.fillMaxWidth().heightIn(min = 220.dp),
          placeholder = { Text("Paste your notes, textbook passage, or lesson text here") },
          supportingText = { Text("${state.studyText.length} characters") },
          minLines = 8,
          maxLines = 16,
        )
      }

      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          Button(
            onClick = {
              focusManager.clearFocus()
              onRead()
            },
            enabled =
              speechReady &&
                state.studyText.isNotBlank() &&
                !state.isImportingScreenshots &&
                !state.isImportingDocument &&
                !state.isAskingTutor &&
                !isNarrationActive,
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text(readButtonLabel)
          }
          OutlinedButton(
            onClick = onStop,
            enabled = isNarrationActive,
            modifier = Modifier.weight(1f).height(48.dp),
          ) {
            Icon(Icons.Rounded.Stop, contentDescription = null)
            Spacer(Modifier.size(8.dp))
            Text("Stop")
          }
        }
        if (playbackControlsEnabled) {
          Row(
            modifier = Modifier.fillMaxWidth().height(48.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            IconButton(onClick = onPreviousSegment, enabled = canSkipPrevious) {
              Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous sentence")
            }
            IconButton(onClick = onPauseResume) {
              Icon(
                if (isSpeechPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                contentDescription = if (isSpeechPaused) "Resume reading" else "Pause reading",
              )
            }
            IconButton(onClick = onNextSegment, enabled = canSkipNext) {
              Icon(Icons.Rounded.SkipNext, contentDescription = "Next sentence")
            }
          }
          LinearProgressIndicator(
            progress = { playbackProgress.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
          )
          Text(playbackPositionLabel, style = MaterialTheme.typography.labelSmall)
        }
        Text(speechMessage, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        when {
          studyTextHighlight != null -> {
            SpeechHighlightStatus(text = state.studyText, highlight = studyTextHighlight)
          }

          explanationHighlight != null -> {
            SpeechHighlightStatus(text = state.explanation, highlight = explanationHighlight)
          }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text("Speed ${String.format(Locale.US, "%.1fx", speechRate)}", modifier = Modifier.weight(0.35f))
          Slider(
            value = speechRate,
            onValueChange = onSpeechRateChange,
            valueRange = 0.6f..1.4f,
            steps = 7,
            modifier = Modifier.weight(0.65f),
          )
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Text("Auto-read AI explanation", style = MaterialTheme.typography.bodyLarge)
          Switch(
            checked = autoReadExplanation,
            onCheckedChange = onAutoReadExplanationChange,
            enabled = !isNarrationActive,
          )
        }
      }

      HorizontalDivider()

      Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Offline AI connection", style = MaterialTheme.typography.titleLarge)
        Text(
          state.statusMessage,
          style = MaterialTheme.typography.bodyLarge,
          color = if (state.errorMessage == null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
        )
        OutlinedTextField(
          value = state.serverUrl,
          onValueChange = onServerUrlChange,
          label = { Text("Computer address") },
          placeholder = { Text("http://192.168.1.100:11434") },
          supportingText = { Text("Phone and computer must use the same Wi-Fi") },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedButton(
          onClick = onTestConnection,
          enabled = !state.isTestingConnection && !state.isExplaining && !state.isAskingTutor && !isNarrationActive,
          modifier = Modifier.fillMaxWidth().height(48.dp),
        ) {
          if (state.isTestingConnection) {
            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
          } else {
            Icon(Icons.Rounded.Wifi, contentDescription = null)
          }
          Spacer(Modifier.size(8.dp))
          Text(if (state.isTestingConnection) "Testing..." else "Test connection")
        }
      }

      state.errorMessage?.let { message ->
        Text(message, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyLarge)
      }

      Button(
        onClick = {
          focusManager.clearFocus()
          onExplain()
        },
        enabled =
          state.studyText.isNotBlank() &&
            !state.isExplaining &&
            !state.isGeneratingFlashcards &&
            !state.isTestingConnection &&
            !state.isImportingScreenshots &&
            !state.isImportingDocument &&
            !state.isAskingTutor &&
            !isNarrationActive,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary),
        modifier = Modifier.fillMaxWidth().height(52.dp),
      ) {
        if (state.isExplaining) {
          CircularProgressIndicator(
            modifier = Modifier.size(22.dp),
            color = MaterialTheme.colorScheme.onTertiary,
            strokeWidth = 2.dp,
          )
        } else {
          Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
        }
        Spacer(Modifier.size(8.dp))
        Text(if (state.isExplaining) "Explaining..." else "Explain with AI", fontWeight = FontWeight.SemiBold)
      }

      OutlinedButton(
        onClick = {
          focusManager.clearFocus()
          onGenerateFlashcards()
        },
        enabled =
          state.studyText.isNotBlank() &&
            !state.isGeneratingFlashcards &&
            !state.isExplaining &&
            !state.isTestingConnection &&
            !state.isImportingScreenshots &&
            !state.isImportingDocument &&
            !state.isAskingTutor &&
            !isNarrationActive,
        modifier = Modifier.fillMaxWidth().height(52.dp),
      ) {
        if (state.isGeneratingFlashcards) {
          CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp)
        } else {
          Icon(Icons.Rounded.CollectionsBookmark, contentDescription = null)
        }
        Spacer(Modifier.size(8.dp))
        Text(
          when {
            state.isGeneratingFlashcards -> "Creating cards..."
            state.flashcards.isNotEmpty() -> "Regenerate study cards"
            else -> "Create study cards"
          },
          fontWeight = FontWeight.SemiBold,
        )
      }

      if (state.explanation.isNotBlank()) {
        HorizontalDivider()
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text("Tutor explanation", style = MaterialTheme.typography.titleLarge)
          SelectionContainer {
            Text(
              highlightedAnnotatedString(
                text = state.explanation,
                highlight = explanationHighlight,
                highlightColor = MaterialTheme.colorScheme.primaryContainer,
              ),
              style = MaterialTheme.typography.bodyLarge,
            )
          }
        }
      } else if (!state.isExplaining) {
        Box(modifier = Modifier.fillMaxWidth().padding(vertical = 20.dp), contentAlignment = Alignment.Center) {
          Text(
            "Your explanation will appear here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }

      if (state.flashcards.isNotEmpty()) {
        HorizontalDivider()
        FlashcardStudyTools(
          flashcards = state.flashcards,
          onUpdateFlashcard = onUpdateFlashcard,
          onDeleteFlashcard = onDeleteFlashcard,
        )
      }

      HorizontalDivider()
      TutorChat(
        state = state,
        enabled =
          state.studyText.isNotBlank() &&
            !state.isExplaining &&
            !state.isGeneratingFlashcards &&
            !state.isTestingConnection &&
            !state.isImportingScreenshots &&
            !state.isImportingDocument &&
            !isNarrationActive,
        onAskTutor = onAskTutor,
      )

      Spacer(Modifier.height(24.dp))
        }
      }
    }
  }
}
}

@Composable
private fun ProfileAvatar(profileImage: ImageBitmap?, size: Dp) {
  Surface(
    modifier = Modifier.size(size),
    shape = CircleShape,
    color = MaterialTheme.colorScheme.secondaryContainer,
  ) {
    if (profileImage != null) {
      Image(
        bitmap = profileImage,
        contentDescription = "Profile photo",
        modifier = Modifier.fillMaxSize().clip(CircleShape),
        contentScale = ContentScale.Crop,
      )
    } else {
      Box(contentAlignment = Alignment.Center) {
        Icon(
          Icons.Rounded.Person,
          contentDescription = "Choose profile photo",
          modifier = Modifier.size(size * 0.62f),
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
    }
  }
}

@Composable
private fun SpeechHighlightStatus(text: String, highlight: TextHighlightRange) {
  val contextStart = (highlight.start - 55).coerceAtLeast(0)
  val contextEnd = (highlight.endExclusive + 90).coerceAtMost(text.length)
  val leadingMarker = if (contextStart > 0) "..." else ""
  val trailingMarker = if (contextEnd < text.length) "..." else ""
  val excerpt = leadingMarker + text.substring(contextStart, contextEnd).replace('\n', ' ') + trailingMarker
  val adjustedHighlight =
    TextHighlightRange(
      start = leadingMarker.length + highlight.start - contextStart,
      endExclusive = leadingMarker.length + highlight.endExclusive - contextStart,
    )

  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.small,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text("Now reading", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
      Text(
        highlightedAnnotatedString(excerpt, adjustedHighlight, MaterialTheme.colorScheme.primaryContainer),
        style = MaterialTheme.typography.bodyMedium,
        maxLines = 4,
      )
    }
  }
}

private fun Voice.toVoiceOption(): VoiceOption =
  VoiceOption(
    name = name,
    label = "${locale.getDisplayName(Locale.getDefault())} - $name",
  )

private class HighlightVisualTransformation(
  private val highlight: TextHighlightRange?,
  private val highlightColor: Color,
) : VisualTransformation {
  override fun filter(text: AnnotatedString): TransformedText =
    TransformedText(
      text = highlightedAnnotatedString(text.text, highlight, highlightColor),
      offsetMapping = OffsetMapping.Identity,
    )
}

private fun highlightedAnnotatedString(
  text: String,
  highlight: TextHighlightRange?,
  highlightColor: Color,
): AnnotatedString =
  AnnotatedString.Builder(text)
    .apply {
      if (highlight != null) {
        val start = highlight.start.coerceIn(0, text.length)
        val end = highlight.endExclusive.coerceIn(start, text.length)
        if (end > start) addStyle(SpanStyle(background = highlightColor), start, end)
      }
    }
    .toAnnotatedString()

@Preview(showBackground = true, widthDp = 390, heightDp = 844)
@Composable
private fun StudyReaderPreview() {
  StudyReaderTheme {
    StudyReaderScreen(
      state =
        MainScreenUiState(
          studyText = "Photosynthesis lets plants use light energy to make glucose from carbon dioxide and water.",
          statusMessage = "Connected - llama3.2 is ready",
        ),
      speechRate = 1f,
      speechReady = true,
      isNarrationActive = false,
      playbackControlsEnabled = false,
      isSpeechPaused = false,
      playbackProgress = 0f,
      playbackPositionLabel = "",
      canSkipPrevious = false,
      canSkipNext = false,
      readButtonLabel = "Read aloud",
      speechMessage = "Phone voice ready",
      studyTextHighlight = null,
      explanationHighlight = null,
      autoReadExplanation = true,
      selectedPalette = StudyPalette.Teal,
      onPaletteSelected = {},
      profileImage = null,
      profileImageError = null,
      onProfileImageClick = {},
      voiceOptions = listOf(VoiceOption("demo", "English - demo")),
      selectedVoiceName = "demo",
      onVoiceSelected = {},
      onPreviewVoice = {},
      onStudyTextChange = {},
      onStudySetTitleChange = {},
      onSaveStudySet = {},
      onNewStudySet = {},
      onOpenStudySet = {},
      onDeleteStudySet = {},
      onAddScreenshots = {},
      onImportDocument = {},
      onTakePhoto = {},
      onServerUrlChange = {},
      onSpeechRateChange = {},
      onAutoReadExplanationChange = {},
      onRead = {},
      onStop = {},
      onPauseResume = {},
      onPreviousSegment = {},
      onNextSegment = {},
      onTestConnection = {},
      onExplain = {},
      onGenerateFlashcards = {},
      onAskTutor = {},
      onUpdateFlashcard = { _, _, _ -> },
      onDeleteFlashcard = {},
    )
  }
}
