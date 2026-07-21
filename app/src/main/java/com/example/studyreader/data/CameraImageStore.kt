package com.example.studyreader.data

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import java.io.File

private const val CAMERA_DIRECTORY = "studyreader-camera"
private const val CAMERA_FILE_PREFIX = "studyreader-photo-"
private const val MAX_CACHED_PHOTO_AGE_MILLIS = 24L * 60L * 60L * 1000L

fun createCameraImageUri(context: Context): Uri {
  val cameraDirectory = File(context.cacheDir, CAMERA_DIRECTORY)
  check(cameraDirectory.exists() || cameraDirectory.mkdirs()) { "Could not prepare camera storage." }
  deleteOldCameraImages(cameraDirectory)

  val imageFile = File.createTempFile(CAMERA_FILE_PREFIX, ".jpg", cameraDirectory)
  return FileProvider.getUriForFile(
    context,
    "${context.packageName}.fileprovider",
    imageFile,
  )
}

fun deleteCameraImage(context: Context, imageUri: Uri) {
  runCatching { context.contentResolver.delete(imageUri, null, null) }
}

private fun deleteOldCameraImages(cameraDirectory: File) {
  val oldestAllowedTimestamp = System.currentTimeMillis() - MAX_CACHED_PHOTO_AGE_MILLIS
  cameraDirectory.listFiles()?.forEach { file ->
    if (file.isFile && file.lastModified() < oldestAllowedTimestamp) file.delete()
  }
}
