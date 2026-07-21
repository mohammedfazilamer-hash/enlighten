package com.example.studyreader.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.NavigateBefore
import androidx.compose.material.icons.automirrored.rounded.NavigateNext
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.studyreader.data.Flashcard

private enum class StudyCardMode(val label: String) {
  Flashcards("Flashcards"),
  Quiz("Quiz"),
}

@Composable
internal fun FlashcardStudyTools(
  flashcards: List<Flashcard>,
  onUpdateFlashcard: (String, String, String) -> Unit,
  onDeleteFlashcard: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (flashcards.isEmpty()) return

  var modeName by rememberSaveable { mutableStateOf(StudyCardMode.Flashcards.name) }
  val mode = StudyCardMode.entries.firstOrNull { it.name == modeName } ?: StudyCardMode.Flashcards
  var currentIndex by rememberSaveable(flashcards.map(Flashcard::id)) { mutableIntStateOf(0) }
  var answerVisible by rememberSaveable(currentIndex, modeName) { mutableStateOf(false) }
  var quizAnswer by rememberSaveable(currentIndex) { mutableStateOf("") }
  var masteredIds by rememberSaveable { mutableStateOf(emptyList<String>()) }
  var editingCard by remember { mutableStateOf<Flashcard?>(null) }

  LaunchedEffect(flashcards.size) {
    if (currentIndex > flashcards.lastIndex) currentIndex = flashcards.lastIndex.coerceAtLeast(0)
    masteredIds = masteredIds.filter { id -> flashcards.any { it.id == id } }
  }

  val currentCard = flashcards[currentIndex.coerceIn(0, flashcards.lastIndex)]

  Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column {
        Text("Study cards", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
        Text(
          if (mode == StudyCardMode.Quiz) "${masteredIds.size} of ${flashcards.size} mastered" else "${flashcards.size} saved cards",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.labelMedium,
        )
      }
      IconButton(onClick = { editingCard = currentCard }) {
        Icon(Icons.Rounded.Edit, contentDescription = "Edit current card")
      }
    }

    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
      StudyCardMode.entries.forEachIndexed { index, option ->
        SegmentedButton(
          selected = mode == option,
          onClick = { modeName = option.name },
          shape = SegmentedButtonDefaults.itemShape(index = index, count = StudyCardMode.entries.size),
          label = { Text(option.label) },
        )
      }
    }

    Surface(
      modifier = Modifier.fillMaxWidth().heightIn(min = 210.dp),
      shape = RoundedCornerShape(8.dp),
      color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
      Column(
        modifier = Modifier.padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
      ) {
        Text("Question ${currentIndex + 1} of ${flashcards.size}", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
        Text(currentCard.question, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

        if (mode == StudyCardMode.Quiz) {
          OutlinedTextField(
            value = quizAnswer,
            onValueChange = { quizAnswer = it },
            label = { Text("Your answer") },
            enabled = !answerVisible,
            minLines = 2,
            modifier = Modifier.fillMaxWidth(),
          )
        }

        if (answerVisible) {
          HorizontalDivider()
          Text("Answer", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.tertiary)
          Text(currentCard.answer, modifier = Modifier.testTag("flashcard-answer"), style = MaterialTheme.typography.bodyLarge)
        } else {
          Button(onClick = { answerVisible = true }, modifier = Modifier.fillMaxWidth()) {
            Text(if (mode == StudyCardMode.Quiz) "Check answer" else "Show answer")
          }
        }

        if (mode == StudyCardMode.Quiz && answerVisible) {
          Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedButton(
              onClick = {
                masteredIds = masteredIds - currentCard.id
                currentIndex = (currentIndex + 1) % flashcards.size
              },
              modifier = Modifier.weight(1f),
            ) {
              Text("Review again")
            }
            Button(
              onClick = {
                masteredIds = (masteredIds + currentCard.id).distinct()
                currentIndex = (currentIndex + 1) % flashcards.size
              },
              modifier = Modifier.weight(1f),
            ) {
              Icon(Icons.Rounded.Check, contentDescription = null)
              Spacer(Modifier.size(6.dp))
              Text("I got it")
            }
          }
        }
      }
    }

    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      IconButton(
        onClick = { currentIndex = if (currentIndex == 0) flashcards.lastIndex else currentIndex - 1 },
      ) {
        Icon(Icons.AutoMirrored.Rounded.NavigateBefore, contentDescription = "Previous card")
      }
      Text("${currentIndex + 1} / ${flashcards.size}", style = MaterialTheme.typography.labelLarge)
      IconButton(
        onClick = { currentIndex = (currentIndex + 1) % flashcards.size },
      ) {
        Icon(Icons.AutoMirrored.Rounded.NavigateNext, contentDescription = "Next card")
      }
    }
  }

  editingCard?.let { card ->
    EditFlashcardDialog(
      flashcard = card,
      onDismiss = { editingCard = null },
      onSave = { question, answer ->
        onUpdateFlashcard(card.id, question, answer)
        editingCard = null
      },
      onDelete = {
        onDeleteFlashcard(card.id)
        editingCard = null
      },
    )
  }
}

@Composable
private fun EditFlashcardDialog(
  flashcard: Flashcard,
  onDismiss: () -> Unit,
  onSave: (String, String) -> Unit,
  onDelete: () -> Unit,
) {
  var question by remember(flashcard.id) { mutableStateOf(flashcard.question) }
  var answer by remember(flashcard.id) { mutableStateOf(flashcard.answer) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Edit study card") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedTextField(
          value = question,
          onValueChange = { question = it },
          label = { Text("Question") },
          minLines = 2,
          modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
          value = answer,
          onValueChange = { answer = it },
          label = { Text("Answer") },
          minLines = 3,
          modifier = Modifier.fillMaxWidth(),
        )
        TextButton(onClick = onDelete) {
          Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = MaterialTheme.colorScheme.error)
          Spacer(Modifier.size(6.dp))
          Text("Delete card", color = MaterialTheme.colorScheme.error)
        }
      }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    confirmButton = {
      TextButton(
        onClick = { onSave(question.trim(), answer.trim()) },
        enabled = question.isNotBlank() && answer.isNotBlank(),
      ) {
        Text("Save")
      }
    },
  )
}
