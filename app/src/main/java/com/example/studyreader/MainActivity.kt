package com.example.studyreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.studyreader.theme.StudyPalette
import com.example.studyreader.theme.StudyReaderTheme

private const val PREFERENCES_NAME = "study_reader"
private const val PALETTE_KEY = "ui_palette"

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      val preferences = remember { getSharedPreferences(PREFERENCES_NAME, 0) }
      var palette by remember {
        mutableStateOf(StudyPalette.fromStoredName(preferences.getString(PALETTE_KEY, null)))
      }
      StudyReaderTheme(palette = palette) {
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
