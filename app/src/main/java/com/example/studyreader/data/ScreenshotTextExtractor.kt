package com.example.studyreader.data

import android.content.Context
import android.net.Uri
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

interface ScreenshotTextExtractor : AutoCloseable {
  suspend fun extractText(imageUri: Uri): String

  override fun close() = Unit
}

class MlKitScreenshotTextExtractor(context: Context) : ScreenshotTextExtractor {
  private val appContext = context.applicationContext
  private val recognizer: TextRecognizer =
    TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

  override suspend fun extractText(imageUri: Uri): String {
    val image = withContext(Dispatchers.IO) { InputImage.fromFilePath(appContext, imageUri) }
    return recognizer.process(image).awaitResult().text.trim()
  }

  override fun close() {
    recognizer.close()
  }
}

internal suspend fun <T> Task<T>.awaitResult(): T =
  suspendCancellableCoroutine { continuation ->
    addOnSuccessListener { result ->
      if (continuation.isActive) continuation.resume(result)
    }
    addOnFailureListener { error ->
      if (continuation.isActive) continuation.resumeWithException(error)
    }
    addOnCanceledListener { continuation.cancel() }
  }
