package com.example.studyreader

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.example.studyreader.ui.main.MainScreen
import com.example.studyreader.theme.StudyPalette

@Composable
fun MainNavigation(
  selectedPalette: StudyPalette,
  onPaletteSelected: (StudyPalette) -> Unit,
) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(
            selectedPalette = selectedPalette,
            onPaletteSelected = onPaletteSelected,
          )
        }
      },
  )
}
