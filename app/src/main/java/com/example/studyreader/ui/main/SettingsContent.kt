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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.RecordVoiceOver
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.studyreader.data.AiProviderMode
import com.example.studyreader.data.OnDeviceModelStatus
import com.example.studyreader.data.ON_DEVICE_MODEL_NAME
import com.example.studyreader.theme.StudyPalette

internal data class VoiceOption(val name: String, val label: String)

internal enum class SpeechProvider(val displayName: String) {
  Phone("Phone"),
  NaturalLocal("Natural local"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SettingsContent(
  selectedPalette: StudyPalette,
  onPaletteSelected: (StudyPalette) -> Unit,
  voiceOptions: List<VoiceOption>,
  selectedVoiceName: String?,
  onVoiceSelected: (String) -> Unit,
  speechProvider: SpeechProvider,
  onSpeechProviderSelected: (SpeechProvider) -> Unit,
  naturalVoiceOptions: List<VoiceOption>,
  selectedNaturalVoiceName: String,
  onNaturalVoiceSelected: (String) -> Unit,
  onPreviewVoice: () -> Unit,
  voiceControlsEnabled: Boolean,
  aiProviderMode: AiProviderMode,
  onAiProviderSelected: (AiProviderMode) -> Unit,
  onDeviceModelStatus: OnDeviceModelStatus,
  isImportingOnDeviceModel: Boolean,
  onDeviceModelImportProgress: Float?,
  onDeviceModelMessage: String?,
  onGetOnDeviceModel: () -> Unit,
  onImportOnDeviceModel: () -> Unit,
  onRemoveOnDeviceModel: () -> Unit,
  modifier: Modifier = Modifier,
) {
  var showVoicePicker by remember { mutableStateOf(false) }
  var showRemoveModelDialog by remember { mutableStateOf(false) }
  val activeVoiceOptions = if (speechProvider == SpeechProvider.Phone) voiceOptions else naturalVoiceOptions
  val activeVoiceName = if (speechProvider == SpeechProvider.Phone) selectedVoiceName else selectedNaturalVoiceName
  val selectedVoice = activeVoiceOptions.firstOrNull { it.name == activeVoiceName }

  Column(
    modifier =
      modifier
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 20.dp, vertical = 16.dp),
    verticalArrangement = Arrangement.spacedBy(18.dp),
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
      Text("AI tutor", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
    }
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      AiProviderMode.entries.forEachIndexed { index, provider ->
        SegmentedButton(
          selected = provider == aiProviderMode,
          onClick = { onAiProviderSelected(provider) },
          enabled = !isImportingOnDeviceModel,
          shape = SegmentedButtonDefaults.itemShape(index = index, count = AiProviderMode.entries.size),
          label = { Text(provider.displayName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        )
      }
    }
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Text(
        ON_DEVICE_MODEL_NAME,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
      )
      Text(
        when {
          isImportingOnDeviceModel -> "Importing to private phone storage"
          onDeviceModelStatus.installed -> "Ready offline | ${formatModelSize(onDeviceModelStatus.sizeBytes)}"
          else -> "Not installed | 584 MB model"
        },
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      onDeviceModelMessage?.let { message ->
        Text(message, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
      }
    }
    if (isImportingOnDeviceModel) {
      if (onDeviceModelImportProgress != null) {
        LinearProgressIndicator(
          progress = { onDeviceModelImportProgress.coerceIn(0f, 1f) },
          modifier = Modifier.fillMaxWidth(),
        )
      } else {
        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
      }
    }
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(
        onClick = onGetOnDeviceModel,
        enabled = !isImportingOnDeviceModel,
        modifier = Modifier.weight(1f).height(48.dp),
      ) {
        Icon(Icons.Rounded.Download, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Get model", maxLines = 1)
      }
      OutlinedButton(
        onClick = onImportOnDeviceModel,
        enabled = !isImportingOnDeviceModel,
        modifier = Modifier.weight(1f).height(48.dp),
      ) {
        Icon(Icons.Rounded.FolderOpen, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text(if (onDeviceModelStatus.installed) "Replace" else "Import", maxLines = 1)
      }
    }
    if (onDeviceModelStatus.installed) {
      TextButton(
        onClick = { showRemoveModelDialog = true },
        enabled = !isImportingOnDeviceModel,
      ) {
        Icon(Icons.Rounded.Delete, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("Remove phone model")
      }
    }

    HorizontalDivider()

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
    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      SpeechProvider.entries.forEachIndexed { index, provider ->
        SegmentedButton(
          selected = provider == speechProvider,
          onClick = {
            showVoicePicker = false
            onSpeechProviderSelected(provider)
          },
          modifier = Modifier.testTag("speech-provider-${provider.name}"),
          shape = SegmentedButtonDefaults.itemShape(index = index, count = SpeechProvider.entries.size),
          label = { Text(provider.displayName) },
        )
      }
    }
    Text(
      selectedVoice?.label
        ?: if (speechProvider == SpeechProvider.Phone && voiceOptions.isEmpty()) {
          "Loading phone voices..."
        } else {
          "Choose a voice"
        },
      style = MaterialTheme.typography.bodyLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
      OutlinedButton(
        onClick = { showVoicePicker = true },
        enabled = voiceControlsEnabled && activeVoiceOptions.isNotEmpty(),
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
          items(activeVoiceOptions, key = VoiceOption::name) { voice ->
            Row(
              modifier =
                Modifier.fillMaxWidth()
                  .clickable {
                    if (speechProvider == SpeechProvider.Phone) {
                      onVoiceSelected(voice.name)
                    } else {
                      onNaturalVoiceSelected(voice.name)
                    }
                    showVoicePicker = false
                  }
                  .padding(vertical = 10.dp),
              verticalAlignment = Alignment.CenterVertically,
            ) {
              RadioButton(
                selected = voice.name == activeVoiceName,
                onClick = {
                  if (speechProvider == SpeechProvider.Phone) {
                    onVoiceSelected(voice.name)
                  } else {
                    onNaturalVoiceSelected(voice.name)
                  }
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

  if (showRemoveModelDialog) {
    AlertDialog(
      onDismissRequest = { showRemoveModelDialog = false },
      title = { Text("Remove phone AI model?") },
      text = { Text("Offline AI will stop working until the model is imported again.") },
      confirmButton = {
        TextButton(
          onClick = {
            showRemoveModelDialog = false
            onRemoveOnDeviceModel()
          },
        ) {
          Text("Remove")
        }
      },
      dismissButton = {
        TextButton(onClick = { showRemoveModelDialog = false }) { Text("Cancel") }
      },
    )
  }
}

private fun formatModelSize(bytes: Long): String = String.format("%.0f MB", bytes / (1024.0 * 1024.0))

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
