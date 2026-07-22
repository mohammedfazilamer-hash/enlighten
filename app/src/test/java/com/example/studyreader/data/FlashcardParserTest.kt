package com.example.studyreader.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FlashcardParserTest {
  @Test
  fun parseFlashcards_acceptsCompactOnDeviceFormat() {
    val cards =
      parseFlashcards(
        """
        Q: What does chlorophyll absorb? | A: Sunlight.
        Q: What sugar do plants make? | A: Glucose.
        """.trimIndent(),
      )

    assertEquals(2, cards.size)
    assertEquals("What does chlorophyll absorb?", cards[0].question)
    assertEquals("Sunlight.", cards[0].answer)
  }

  @Test
  fun parseFlashcards_acceptsNumberedQuestionAndAnswerLines() {
    val cards =
      parseFlashcards(
        """
        1. Question: What is photosynthesis?
        Answer: The process plants use to turn light into chemical energy.
        2. Q: Which pigment absorbs light?
        A: Chlorophyll.
        """.trimIndent(),
      )

    assertEquals(2, cards.size)
    assertEquals("Chlorophyll.", cards[1].answer)
  }

  @Test
  fun buildOnDeviceFlashcardPrompt_requestsEasyToFollowLineFormat() {
    val prompt = buildOnDeviceFlashcardPrompt("Plants use sunlight.")

    assertTrue(prompt.contains("Q: Question | A: Answer"))
    assertTrue(prompt.endsWith("Plants use sunlight."))
  }
}
