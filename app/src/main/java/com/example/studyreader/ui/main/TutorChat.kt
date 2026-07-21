package com.example.studyreader.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studyreader.data.TutorMessageRole

@Composable
internal fun TutorChat(
  state: MainScreenUiState,
  enabled: Boolean,
  onAskTutor: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var question by rememberSaveable { mutableStateOf("") }

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Text("Ask your tutor", style = MaterialTheme.typography.titleLarge)

    state.tutorMessages.forEach { message ->
      val isStudent = message.role == TutorMessageRole.Student
      Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = if (isStudent) Alignment.CenterEnd else Alignment.CenterStart,
      ) {
        Surface(
          modifier = Modifier.fillMaxWidth(0.88f).widthIn(max = 520.dp),
          shape = MaterialTheme.shapes.small,
          color =
            if (isStudent) {
              MaterialTheme.colorScheme.primaryContainer
            } else {
              MaterialTheme.colorScheme.surfaceContainerHigh
            },
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
              if (isStudent) "You" else "Tutor",
              style = MaterialTheme.typography.labelMedium,
              fontWeight = FontWeight.SemiBold,
              color = MaterialTheme.colorScheme.primary,
            )
            Text(message.text, style = MaterialTheme.typography.bodyLarge)
          }
        }
      }
    }

    if (state.isAskingTutor) {
      Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text("Tutor is thinking...", style = MaterialTheme.typography.labelMedium)
      }
    }

    OutlinedTextField(
      value = question,
      onValueChange = { question = it },
      enabled = enabled && !state.isAskingTutor,
      label = { Text("Ask about this passage") },
      minLines = 2,
      maxLines = 5,
      trailingIcon = {
        IconButton(
          onClick = {
            onAskTutor(question)
            question = ""
          },
          enabled = enabled && !state.isAskingTutor && question.isNotBlank(),
          modifier = Modifier.testTag("send-tutor-question"),
        ) {
          Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Send question")
        }
      },
      modifier = Modifier.fillMaxWidth(),
    )
  }
}
