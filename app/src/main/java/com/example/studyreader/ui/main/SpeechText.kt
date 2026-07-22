package com.example.studyreader.ui.main

import java.text.BreakIterator
import java.util.Locale

data class SpeechChunk(
  val text: String,
  val startOffset: Int,
  val endOffset: Int,
)

fun splitTextForSpeech(text: String, maxLength: Int = 3_500): List<String> =
  splitTextForSpeechRanges(text, maxLength).map(SpeechChunk::text)

fun splitTextForSpeechRanges(text: String, maxLength: Int = 3_500): List<SpeechChunk> {
  require(maxLength > 0)
  if (text.isBlank()) return emptyList()

  val chunks = mutableListOf<SpeechChunk>()
  var chunkStart = text.indexOfFirst { !it.isWhitespace() }.coerceAtLeast(0)
  while (chunkStart < text.length) {
    val maximumEnd = minOf(chunkStart + maxLength, text.length)
    var rawEnd = maximumEnd
    if (maximumEnd < text.length) {
      val newlineCut = text.lastIndexOf('\n', maximumEnd)
      val spaceCut = text.lastIndexOf(' ', maximumEnd)
      val naturalCut = maxOf(newlineCut, spaceCut)
      if (naturalCut >= chunkStart + maxLength / 2) rawEnd = naturalCut
    }

    var chunkEnd = rawEnd
    while (chunkEnd > chunkStart && text[chunkEnd - 1].isWhitespace()) chunkEnd--
    if (chunkEnd > chunkStart) {
      chunks +=
        SpeechChunk(
          text = text.substring(chunkStart, chunkEnd),
          startOffset = chunkStart,
          endOffset = chunkEnd,
        )
    }

    chunkStart = rawEnd
    while (chunkStart < text.length && text[chunkStart].isWhitespace()) chunkStart++
  }
  return chunks
}

fun splitTextForSpeechSegments(text: String, maxLength: Int = 900): List<SpeechChunk> {
  require(maxLength > 0)
  if (text.isBlank()) return emptyList()

  val sentenceIterator = BreakIterator.getSentenceInstance(Locale.getDefault()).apply { setText(text) }
  val segments = mutableListOf<SpeechChunk>()
  var rawStart = sentenceIterator.first()
  var rawEnd = sentenceIterator.next()
  while (rawEnd != BreakIterator.DONE) {
    var start = rawStart
    var end = rawEnd
    while (start < end && text[start].isWhitespace()) start++
    while (end > start && text[end - 1].isWhitespace()) end--
    if (end > start) {
      splitTextForSpeechRanges(text.substring(start, end), maxLength).forEach { chunk ->
        segments +=
          SpeechChunk(
            text = chunk.text,
            startOffset = start + chunk.startOffset,
            endOffset = start + chunk.endOffset,
          )
      }
    }
    rawStart = rawEnd
    rawEnd = sentenceIterator.next()
  }
  return segments
}

fun splitTextForNaturalVoiceSegments(
  text: String,
  maxLength: Int = 900,
  targetLength: Int = 320,
): List<SpeechChunk> {
  require(maxLength > 0)
  require(targetLength in 1..maxLength)
  val sentences = splitTextForSpeechSegments(text, maxLength)
  if (sentences.isEmpty()) return emptyList()

  val groups = mutableListOf<SpeechChunk>()
  var sentenceIndex = 0
  while (sentenceIndex < sentences.size) {
    val startOffset = sentences[sentenceIndex].startOffset
    var endOffset = sentences[sentenceIndex].endOffset
    var nextIndex = sentenceIndex + 1
    while (
      nextIndex < sentences.size &&
        endOffset - startOffset < targetLength &&
        sentences[nextIndex].endOffset - startOffset <= maxLength
    ) {
      endOffset = sentences[nextIndex].endOffset
      nextIndex++
    }
    groups +=
      SpeechChunk(
        text = text.substring(startOffset, endOffset),
        startOffset = startOffset,
        endOffset = endOffset,
      )
    sentenceIndex = nextIndex
  }
  return groups
}
