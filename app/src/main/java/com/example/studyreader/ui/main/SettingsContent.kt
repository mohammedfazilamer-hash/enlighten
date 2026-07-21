package com.example.studyreader.ui.main

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.studyreader.theme.StudyPalette

internal data class VoiceOption(val name: String, val label: String)

@Composable
internal fun SettingsContent(
  selectedPalette: StudyPalette,
  onPaletteSelected: (StudyPalette) -> Unit,
  voiceOptions: List<VoiceOption>,
  selectedVoiceName: String?,
  onVoiceSelected: (String) -> Unit,
  onPreviewVoice: () -> Unit,
  voiceControlsEnabled: Boolean,
  modifier: Modifier = Modifier,
) {
  var showVoicePicker by remember { mutableStateOf(false) }
  val selectedVoice = voiceOptions.firstOrNull { it.name == selectedVoiceName }

  Column(
    modifier =
      modifier
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Icon(Icons.Rounded.Palette, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Text("Interface color", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      StudyPalette.entries.forEach { palette ->
        PaletteSwatch(
          palette = palette,
          selected = palette == selectedPalette,
          onClick = { onPaletteSelected(palette) },
        )
      }
    }

    HorizontalDivider()

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Icon(Icons.Rounded.RecordVoiceOver, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Text("Reading voice", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
    Text(
      selectedVoice?.label ?: if (voiceOptions.isEmpty()) "Loading phone voices..." else "Phone default voice",
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(
        onClick = { showVoicePicker = true },
        enabled = voiceControlsEnabled && voiceOptions.isNotEmpty(),
        modifier = Modifier.weight(1f).height(48.dp),
      ) {
        Text("Choose voice", maxLines = 1, overflow = TextOverflow.Ellipsis)
      }
      OutlinedButton(
        onClick = onPreviewVoice,
        enabled = voiceControlsEnabled && selectedVoice != null,
        modifier = Modifier.weight(1f).height(48.dp),
      ) {
        Icon(Icons.AutoMirrored.Rounded.VolumeUp, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Preview")
      }
    }
    Spacer(Modifier.height(24.dp))
  }

  if (showVoicePicker) {
    AlertDialog(
      onDismissRequest = { showVoicePicker = false },
      title = { Text("Choose reading voice") },
      text = {
        LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
          items(voiceOptions, key = VoiceOption::name) { voice ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    onVoiceSelected(voice.name)
                    showVoicePicker = false
                  }
                  .padding(vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = voice.name == selectedVoiceName,
                onClick = {
                  onVoiceSelected(voice.name)
                  showVoicePicker = false
                },
              )
              Text(voice.label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showVoicePicker = false }) { Text("Close") }
      },
    )
  }
}

@Composable
private fun PaletteSwatch(
  palette: StudyPalette,
  selected: Boolean,
  onClick: () -> Unit,
) {
  Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
    Surface(
      onClick = onClick,
      modifier = Modifier.size(48.dp),
      shape = CircleShape,
      color = palette.swatch,
      border =
        BorderStroke(
          width = if (selected) 3.dp else 1.dp,
          color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline,
        ),
    ) {
      Box(contentAlignment = Alignment.Center) {
        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White)
      }
    }
    Text(palette.displayName, style = MaterialTheme.typography.labelSmall)
  }
}
