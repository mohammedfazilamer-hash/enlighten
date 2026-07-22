package com.example.studyreader.ui.main

import com.example.studyreader.data.buildStudyPrompt
import com.example.studyreader.data.buildTutorQuestionPrompt
import com.example.studyreader.data.AiExecutionProvider
import com.example.studyreader.data.AiProviderMode
import com.example.studyreader.data.deriveStudySetTitle
import com.example.studyreader.data.extractDocxText
import com.example.studyreader.data.normalizeOllamaBaseUrl
import com.example.studyreader.data.providerOrder
import com.example.studyreader.data.TutorMessage
import com.example.studyreader.data.TutorMessageRole
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MainScreenViewModelTest {
  @Test
  fun providerOrder_autoPrefersInstalledPhoneModelAndKeepsComputerFallback() {
    assertEquals(
      listOf(AiExecutionProvider.OnDevice, AiExecutionProvider.Computer),
      providerOrder(AiProviderMode.Automatic, onDeviceModelInstalled = true),
    )
  }

  @Test
  fun providerOrder_phoneModeNeverSendsStudyTextToComputer() {
    assertEquals(
      listOf(AiExecutionProvider.OnDevice),
      providerOrder(AiProviderMode.OnDevice, onDeviceModelInstalled = false),
    )
  }

  @Test
  fun providerOrder_autoUsesComputerUntilPhoneModelIsInstalled() {
    assertEquals(
      listOf(AiExecutionProvider.Computer),
      providerOrder(AiProviderMode.Automatic, onDeviceModelInstalled = false),
    )
  }

  @Test
  fun normalizeOllamaBaseUrl_addsSchemeAndRemovesTrailingSlash() {
    assertEquals("http://10.0.0.42:11434", normalizeOllamaBaseUrl("10.0.0.42:11434/"))
  }

  @Test
  fun buildStudyPrompt_containsTutorSectionsAndText() {
    val prompt = buildStudyPrompt("Cells are the basic units of life.")

    assertTrue(prompt.contains("1. Simple explanation"))
    assertTrue(prompt.contains("4. Three quiz questions"))
    assertTrue(prompt.endsWith("Cells are the basic units of life."))
  }

  @Test
  fun splitTextForSpeech_keepsEveryChunkWithinLimit() {
    val chunks = splitTextForSpeech("one two three four five six seven", maxLength = 10)

    assertEquals("one two", chunks.first())
    assertTrue(chunks.all { it.length <= 10 })
    assertEquals("one two three four five six seven", chunks.joinToString(" "))
  }

  @Test
  fun splitTextForSpeechRanges_keepsOriginalOffsets() {
    val source = "  one two three four five  "
    val chunks = splitTextForSpeechRanges(source, maxLength = 10)

    assertTrue(chunks.all { source.substring(it.startOffset, it.endOffset) == it.text })
    assertEquals("one two three four five", chunks.joinToString(" ") { it.text })
  }

  @Test
  fun splitTextForSpeechSegments_usesSentenceBoundariesAndOffsets() {
    val source = "First sentence. Second sentence! Third question?"
    val segments = splitTextForSpeechSegments(source)

    assertEquals(listOf("First sentence.", "Second sentence!", "Third question?"), segments.map { it.text })
    assertTrue(segments.all { source.substring(it.startOffset, it.endOffset) == it.text })
  }

  @Test
  fun buildTutorQuestionPrompt_includesPassageQuestionAndRecentHistory() {
    val prompt =
      buildTutorQuestionPrompt(
        studyText = "Cells contain DNA.",
        history = listOf(TutorMessage(role = TutorMessageRole.Student, text = "What do cells contain?")),
        question = "What does DNA do?",
      )

    assertTrue(prompt.contains("Cells contain DNA."))
    assertTrue(prompt.contains("Student: What do cells contain?"))
    assertTrue(prompt.endsWith("What does DNA do?"))
  }

  @Test
  fun extractDocxText_preservesParagraphs() {
    val output = ByteArrayOutputStream()
    ZipOutputStream(output).use { zip ->
      zip.putNextEntry(ZipEntry("word/document.xml"))
      zip.write(
        """<?xml version="1.0" encoding="UTF-8"?>
          <w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">
            <w:body><w:p><w:r><w:t>First paragraph.</w:t></w:r></w:p>
            <w:p><w:r><w:t>Second paragraph.</w:t></w:r></w:p></w:body>
          </w:document>""".trimIndent().toByteArray(),
      )
      zip.closeEntry()
    }

    assertEquals(
      "First paragraph.\nSecond paragraph.",
      extractDocxText(ByteArrayInputStream(output.toByteArray())),
    )
  }

  @Test
  fun deriveStudySetTitle_usesFirstNonBlankLineAndLimitsLength() {
    assertEquals("Biology notes", deriveStudySetTitle("\n Biology notes\nCells are alive."))
    assertTrue(deriveStudySetTitle("a".repeat(80)).length <= 51)
  }

  @Test
  fun mergeStudyText_appendsScreenshotTextInSelectionOrder() {
    val merged = mergeStudyText("Existing notes", listOf("First screenshot", "Second screenshot"))

    assertEquals("Existing notes\n\nFirst screenshot\n\nSecond screenshot", merged)
  }

  @Test
  fun mergeStudyText_ignoresBlankScreenshots() {
    assertEquals("Readable text", mergeStudyText("", listOf("  ", "Readable text")))
  }
}
