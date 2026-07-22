package com.example.studyreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.studyreader.theme.StudyPalette
import com.example.studyreader.theme.StudyReaderTheme
import kotlinx.coroutines.delay

private const val PREFERENCES_NAME = "study_reader"
private const val PALETTE_KEY = "ui_palette"
private const val LAUNCH_ARTWORK_DURATION_MS = 1_600L

class MainActivity : ComponentActivity() {
  private var launchArtworkVisible = false

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val preferences = remember { getSharedPreferences(PREFERENCES_NAME, 0) }
      var showLaunchArtwork by remember { mutableStateOf(savedInstanceState == null) }
      var palette by remember {
        mutableStateOf(StudyPalette.fromStoredName(preferences.getString(PALETTE_KEY, null)))
      }

      LaunchedEffect(showLaunchArtwork) {
        if (showLaunchArtwork) {
          launchArtworkVisible = true
          delay(100)
          hideSystemBars()
          delay(LAUNCH_ARTWORK_DURATION_MS)
          showLaunchArtwork = false
        } else {
          launchArtworkVisible = false
          showSystemBars()
        }
      }

      StudyReaderTheme(palette = palette) {
        if (showLaunchArtwork) {
          Image(
            painter = painterResource(R.drawable.enlighten_launch_artwork),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            MainNavigation(
              selectedPalette = palette,
              onPaletteSelected = { selected ->
                palette = selected
                preferences.edit().putString(PALETTE_KEY, selected.name).apply()
              },
            )
          }
        }
      }
    }
  }

  override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus && launchArtworkVisible) hideSystemBars()
  }

  private fun hideSystemBars() {
    WindowCompat.getInsetsController(window, window.decorView).apply {
      systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
      hide(WindowInsetsCompat.Type.systemBars())
    }
  }

  private fun showSystemBars() {
    WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
  }
}
