package com.example.studyreader.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class StudyPalette(val displayName: String, val swatch: Color) {
  Teal("Teal", Color(0xFF008577)),
  Forest("Forest", Color(0xFF3F7A45)),
  Rose("Rose", Color(0xFFB24F6B)),
  Blue("Blue", Color(0xFF3E6FA8));

  companion object {
    fun fromStoredName(value: String?): StudyPalette = entries.firstOrNull { it.name == value } ?: Teal
  }
}

private data class PaletteColors(
  val lightPrimary: Color,
  val lightPrimaryContainer: Color,
  val lightOnPrimaryContainer: Color,
  val darkPrimary: Color,
  val darkPrimaryContainer: Color,
  val darkOnPrimaryContainer: Color,
  val lightSecondary: Color,
  val lightSecondaryContainer: Color,
  val lightOnSecondaryContainer: Color,
  val darkSecondary: Color,
  val darkSecondaryContainer: Color,
  val darkOnSecondaryContainer: Color,
  val lightTertiary: Color,
  val lightTertiaryContainer: Color,
  val lightOnTertiaryContainer: Color,
  val darkTertiary: Color,
  val darkTertiaryContainer: Color,
  val darkOnTertiaryContainer: Color,
)

private fun paletteColors(palette: StudyPalette): PaletteColors =
  when (palette) {
    StudyPalette.Teal ->
      PaletteColors(
        lightPrimary = TealDark,
        lightPrimaryContainer = Color(0xFFA8F2DF),
        lightOnPrimaryContainer = Color(0xFF002019),
        darkPrimary = TealLight,
        darkPrimaryContainer = Color(0xFF005144),
        darkOnPrimaryContainer = Color(0xFFA8F2DF),
        lightSecondary = GoldDark,
        lightSecondaryContainer = Color(0xFFFFE08F),
        lightOnSecondaryContainer = Color(0xFF241A00),
        darkSecondary = GoldLight,
        darkSecondaryContainer = Color(0xFF594400),
        darkOnSecondaryContainer = Color(0xFFFFE08F),
        lightTertiary = RoseDark,
        lightTertiaryContainer = Color(0xFFFFD9E2),
        lightOnTertiaryContainer = Color(0xFF3E001D),
        darkTertiary = RoseLight,
        darkTertiaryContainer = Color(0xFF733044),
        darkOnTertiaryContainer = Color(0xFFFFD9E2),
      )

    StudyPalette.Forest ->
      PaletteColors(
        lightPrimary = Color(0xFF336B35),
        lightPrimaryContainer = Color(0xFFB6F2B5),
        lightOnPrimaryContainer = Color(0xFF002106),
        darkPrimary = Color(0xFF9AD29A),
        darkPrimaryContainer = Color(0xFF1B511F),
        darkOnPrimaryContainer = Color(0xFFB6F2B5),
        lightSecondary = Color(0xFF745B00),
        lightSecondaryContainer = Color(0xFFFFE08A),
        lightOnSecondaryContainer = Color(0xFF241A00),
        darkSecondary = Color(0xFFE8C36B),
        darkSecondaryContainer = Color(0xFF574500),
        darkOnSecondaryContainer = Color(0xFFFFE08A),
        lightTertiary = Color(0xFF8B4557),
        lightTertiaryContainer = Color(0xFFFFD9E0),
        lightOnTertiaryContainer = Color(0xFF3A0719),
        darkTertiary = Color(0xFFFFB1C1),
        darkTertiaryContainer = Color(0xFF6F2E40),
        darkOnTertiaryContainer = Color(0xFFFFD9E0),
      )

    StudyPalette.Rose ->
      PaletteColors(
        lightPrimary = RoseDark,
        lightPrimaryContainer = Color(0xFFFFD9E2),
        lightOnPrimaryContainer = Color(0xFF3E001D),
        darkPrimary = RoseLight,
        darkPrimaryContainer = Color(0xFF733044),
        darkOnPrimaryContainer = Color(0xFFFFD9E2),
        lightSecondary = TealDark,
        lightSecondaryContainer = Color(0xFFA8F2DF),
        lightOnSecondaryContainer = Color(0xFF002019),
        darkSecondary = TealLight,
        darkSecondaryContainer = Color(0xFF005144),
        darkOnSecondaryContainer = Color(0xFFA8F2DF),
        lightTertiary = GoldDark,
        lightTertiaryContainer = Color(0xFFFFE08F),
        lightOnTertiaryContainer = Color(0xFF241A00),
        darkTertiary = GoldLight,
        darkTertiaryContainer = Color(0xFF594400),
        darkOnTertiaryContainer = Color(0xFFFFE08F),
      )

    StudyPalette.Blue ->
      PaletteColors(
        lightPrimary = Color(0xFF315F93),
        lightPrimaryContainer = Color(0xFFD4E3FF),
        lightOnPrimaryContainer = Color(0xFF001C38),
        darkPrimary = Color(0xFFA8CAFF),
        darkPrimaryContainer = Color(0xFF164776),
        darkOnPrimaryContainer = Color(0xFFD4E3FF),
        lightSecondary = Color(0xFF7A5600),
        lightSecondaryContainer = Color(0xFFFFDEA0),
        lightOnSecondaryContainer = Color(0xFF271900),
        darkSecondary = Color(0xFFF5C66C),
        darkSecondaryContainer = Color(0xFF5C4100),
        darkOnSecondaryContainer = Color(0xFFFFDEA0),
        lightTertiary = Color(0xFF8E4658),
        lightTertiaryContainer = Color(0xFFFFD9E0),
        lightOnTertiaryContainer = Color(0xFF3A0719),
        darkTertiary = Color(0xFFFFB1C1),
        darkTertiaryContainer = Color(0xFF713042),
        darkOnTertiaryContainer = Color(0xFFFFD9E0),
      )
  }

private fun lightScheme(colors: PaletteColors): ColorScheme =
  lightColorScheme(
    primary = colors.lightPrimary,
    onPrimary = Color.White,
    primaryContainer = colors.lightPrimaryContainer,
    onPrimaryContainer = colors.lightOnPrimaryContainer,
    secondary = colors.lightSecondary,
    onSecondary = Color.White,
    secondaryContainer = colors.lightSecondaryContainer,
    onSecondaryContainer = colors.lightOnSecondaryContainer,
    tertiary = colors.lightTertiary,
    onTertiary = Color.White,
    tertiaryContainer = colors.lightTertiaryContainer,
    onTertiaryContainer = colors.lightOnTertiaryContainer,
    background = Paper,
    surface = Color.White,
    onBackground = Ink,
    onSurface = Ink,
    surfaceVariant = Color(0xFFE2E8E4),
    onSurfaceVariant = Color(0xFF414945),
    outline = Color(0xFF717974),
  )

private fun darkScheme(colors: PaletteColors): ColorScheme =
  darkColorScheme(
    primary = colors.darkPrimary,
    onPrimary = InkDark,
    primaryContainer = colors.darkPrimaryContainer,
    onPrimaryContainer = colors.darkOnPrimaryContainer,
    secondary = colors.darkSecondary,
    onSecondary = InkDark,
    secondaryContainer = colors.darkSecondaryContainer,
    onSecondaryContainer = colors.darkOnSecondaryContainer,
    tertiary = colors.darkTertiary,
    onTertiary = InkDark,
    tertiaryContainer = colors.darkTertiaryContainer,
    onTertiaryContainer = colors.darkOnTertiaryContainer,
    background = InkDark,
    surface = SurfaceDark,
    onBackground = Color(0xFFE1E3E0),
    onSurface = Color(0xFFE1E3E0),
    surfaceVariant = Color(0xFF414845),
    onSurfaceVariant = Color(0xFFC1C9C4),
    outline = Color(0xFF8B938E),
  )

@Composable
fun StudyReaderTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  palette: StudyPalette = StudyPalette.Teal,
  content: @Composable () -> Unit,
) {
  val colors = paletteColors(palette)
  MaterialTheme(
    colorScheme = if (darkTheme) darkScheme(colors) else lightScheme(colors),
    typography = Typography,
    content = content,
  )
}
