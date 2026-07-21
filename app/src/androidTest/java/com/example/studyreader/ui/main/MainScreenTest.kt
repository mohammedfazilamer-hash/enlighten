package com.example.studyreader.ui.main

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.test.platform.app.InstrumentationRegistry
import com.example.studyreader.data.createCameraImageUri
import com.example.studyreader.data.FileStudySetStore
import com.example.studyreader.data.Flashcard
import com.example.studyreader.data.ProfileImageStore
import com.example.studyreader.data.StudySet
import com.example.studyreader.data.TutorMessage
import com.example.studyreader.data.TutorMessageRole
import com.example.studyreader.data.deleteCameraImage
import com.example.studyreader.theme.StudyPalette
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.UUID
import java.io.File
import kotlinx.coroutines.runBlocking

class MainScreenTest {
  @get:Rule val composeTestRule = createAndroidComposeRule<ComponentActivity>()

  private var screenState by mutableStateOf(defaultScreenState())
  private var saveStudySetAction: () -> Unit = {}

  @Before
  fun setup() {
    screenState = defaultScreenState()
    saveStudySetAction = {}
    showScreen()
  }

  private fun showScreen() {
    composeTestRule.setContent {
      StudyReaderScreen(
        state = screenState,
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
        onSaveStudySet = { saveStudySetAction() },
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

  @Test
  fun coreStudyActionsAreVisible() {
    composeTestRule.onNodeWithContentDescription("Open menu").assertExists()
    composeTestRule.onNodeWithText("Screenshots").assertIsEnabled()
    composeTestRule.onNodeWithText("Camera").assertIsEnabled()
    composeTestRule.onNodeWithText("PDF or document").assertIsEnabled()
    composeTestRule.onNodeWithText("Auto-read AI explanation").assertExists()
    composeTestRule.onNodeWithText("Read aloud").assertIsEnabled()
    composeTestRule.onNodeWithText("Test connection").assertExists()
    composeTestRule.onNodeWithText("Explain with AI").assertIsEnabled()
    composeTestRule.onNodeWithText("Create study cards").assertIsEnabled()
    composeTestRule.onNodeWithText("Ask your tutor").performScrollTo().assertExists()
    composeTestRule.onNodeWithText("Ask about this passage").assertExists()
  }

  @Test
  fun studySetCanBeSavedAndLibraryOpened() {
    var saveRequested = false
    composeTestRule.runOnIdle { saveStudySetAction = { saveRequested = true } }

    composeTestRule.onNodeWithText("Save set").performClick()
    composeTestRule.runOnIdle { assertTrue(saveRequested) }
    composeTestRule.onNodeWithTag("open-library").performClick()
    composeTestRule.onNodeWithText("Saved study sets").assertExists()
    composeTestRule.onNodeWithText("No saved study sets").assertExists()
  }

  @Test
  fun generatedCardsSupportFlashcardAndQuizModes() {
    composeTestRule.runOnIdle {
      screenState =
        MainScreenUiState(
          studyText = "Plants use light energy.",
          flashcards = listOf(Flashcard(id = "card-1", question = "What do plants use?", answer = "Light energy.")),
          isLoadingStudySets = false,
        )
    }

    composeTestRule.onNodeWithText("Question 1 of 1").assertExists()
    composeTestRule.onNodeWithText("Show answer").performScrollTo().performClick()
    composeTestRule.onNodeWithTag("flashcard-answer", useUnmergedTree = true).assertExists()
    composeTestRule.onNodeWithText("Quiz").performScrollTo().performClick()
    composeTestRule.onNodeWithText("Your answer").assertExists()
  }

  @Test
  fun drawerNavigatesToSettings() {
    composeTestRule.onNodeWithContentDescription("Open menu").performClick()
    composeTestRule.onNodeWithText("Settings").performClick()

    composeTestRule.onNodeWithText("Interface color").assertExists()
    composeTestRule.onNodeWithText("Reading voice").assertExists()
    composeTestRule.onNodeWithText("Choose voice").assertIsEnabled()
    composeTestRule.onNodeWithText("Choose voice").performClick()
    composeTestRule.onNodeWithText("Choose reading voice").assertExists()
  }

  @Test
  fun cameraImageUriUsesPrivateFileProvider() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val imageUri = createCameraImageUri(context)

    assertEquals("content", imageUri.scheme)
    assertEquals("${context.packageName}.fileprovider", imageUri.authority)
    context.contentResolver.openOutputStream(imageUri).use { stream ->
      requireNotNull(stream).write(byteArrayOf(1, 2, 3))
    }

    deleteCameraImage(context, imageUri)
  }

  @Test
  fun profileImageIsStoredPrivatelyAndResized() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val sourceUri = createCameraImageUri(context)
    val sourceBitmap = Bitmap.createBitmap(700, 600, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.GREEN) }
    val testDirectory = File(context.cacheDir, "profile-store-test-${UUID.randomUUID()}")
    val profileImageStore = ProfileImageStore(testDirectory)

    try {
      context.contentResolver.openOutputStream(sourceUri).use { output ->
        checkNotNull(output)
        assertTrue(sourceBitmap.compress(Bitmap.CompressFormat.PNG, 100, output))
      }
      runBlocking { profileImageStore.save(context, sourceUri) }
      assertTrue(profileImageStore.exists())
      val storedBitmap = ProfileImageStore(testDirectory).load()

      assertNotNull(storedBitmap)
      assertTrue(maxOf(storedBitmap!!.width, storedBitmap.height) <= 512)
      storedBitmap.recycle()
    } finally {
      sourceBitmap.recycle()
      profileImageStore.delete()
      testDirectory.deleteRecursively()
      deleteCameraImage(context, sourceUri)
    }
  }

  @Test
  fun studySetStorePersistsCardsAndDeletesOnlyRequestedSet() {
    val context = InstrumentationRegistry.getInstrumentation().targetContext
    val store = FileStudySetStore(context)
    val id = "test-${UUID.randomUUID()}"
    val studySet =
      StudySet(
        id = id,
        title = "Persistence test",
        studyText = "Stored study text",
        explanation = "Stored explanation",
        flashcards = listOf(Flashcard(id = "saved-card", question = "Stored question?", answer = "Stored answer.")),
        tutorMessages = listOf(TutorMessage(id = "saved-message", role = TutorMessageRole.Student, text = "Stored question")),
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis(),
      )

    runBlocking {
      try {
        store.saveStudySet(studySet)
        val restored = store.loadStudySets().first { it.id == id }

        assertEquals(studySet.title, restored.title)
        assertEquals(studySet.studyText, restored.studyText)
        assertEquals(studySet.flashcards, restored.flashcards)
        assertEquals(studySet.tutorMessages, restored.tutorMessages)
      } finally {
        store.deleteStudySet(id)
      }
    }
  }

  companion object {
    private fun defaultScreenState() =
      MainScreenUiState(studyText = "Sample study text", isLoadingStudySets = false)
  }
}
