package com.example.studyreader.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.AtomicFile
import java.io.File
import kotlin.math.ceil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext

private const val PROFILE_DIRECTORY = "profile"
private const val PROFILE_FILE_NAME = "profile.jpg"
private const val MAX_PROFILE_DIMENSION = 512
private const val MAX_DECODE_DIMENSION = 1024

class ProfileImageStore internal constructor(private val directory: File) {
  constructor(context: Context) : this(File(context.filesDir, PROFILE_DIRECTORY))

  private val atomicFile = AtomicFile(File(directory, PROFILE_FILE_NAME))

  suspend fun save(context: Context, imageUri: Uri) {
    withContext(Dispatchers.IO + NonCancellable) {
      val decoded = decodeProfileBitmap(context, imageUri) ?: error("The selected profile image could not be read.")
      val bitmap = decoded.scaledDown(MAX_PROFILE_DIMENSION)
      try {
        check(directory.exists() || directory.mkdirs()) { "Could not prepare profile image storage." }
        val output = atomicFile.startWrite()
        try {
          check(bitmap.compress(Bitmap.CompressFormat.JPEG, 90, output)) { "Could not save the profile image." }
          output.flush()
          atomicFile.finishWrite(output)
        } catch (error: Exception) {
          atomicFile.failWrite(output)
          throw error
        }
      } finally {
        if (bitmap !== decoded) bitmap.recycle()
        decoded.recycle()
      }
    }
  }

  fun load(): Bitmap? {
    val backupFile = File(atomicFile.baseFile.path + ".bak")
    if (!atomicFile.baseFile.exists() && !backupFile.exists()) return null
    return runCatching {
        atomicFile.openRead().use(BitmapFactory::decodeStream)
      }
      .getOrNull()
  }

  fun exists(): Boolean =
    atomicFile.baseFile.exists() || File(atomicFile.baseFile.path + ".bak").exists()

  fun delete() {
    atomicFile.delete()
  }
}

private fun decodeProfileBitmap(context: Context, imageUri: Uri): Bitmap? =
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, imageUri)) { decoder, info, _ ->
      decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
      val largestDimension = maxOf(info.size.width, info.size.height)
      if (largestDimension > MAX_DECODE_DIMENSION) {
        decoder.setTargetSampleSize(ceil(largestDimension.toDouble() / MAX_DECODE_DIMENSION).toInt())
      }
    }
  } else {
    context.contentResolver.openInputStream(imageUri)?.use(BitmapFactory::decodeStream)
  }

private fun Bitmap.scaledDown(maxDimension: Int): Bitmap {
  val largestDimension = maxOf(width, height)
  if (largestDimension <= maxDimension) return this
  val scale = maxDimension.toFloat() / largestDimension
  return Bitmap.createScaledBitmap(
    this,
    (width * scale).toInt().coerceAtLeast(1),
    (height * scale).toInt().coerceAtLeast(1),
    true,
  )
}
