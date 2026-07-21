package com.example.studyreader.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.CollectionsBookmark
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.studyreader.data.StudySet
import java.text.DateFormat
import java.util.Date

@Composable
internal fun StudySetLibrary(
  studySets: List<StudySet>,
  activeStudySetId: String?,
  hasUnsavedChanges: Boolean,
  isLoading: Boolean,
  message: String?,
  onNewStudySet: () -> Unit,
  onOpenStudySet: (String) -> Unit,
  onDeleteStudySet: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var pendingOpenId by remember { mutableStateOf<String?>(null) }
  var pendingDeleteSet by remember { mutableStateOf<StudySet?>(null) }
  var confirmNew by remember { mutableStateOf(false) }

  fun requestOpen(id: String) {
    if (hasUnsavedChanges && id != activeStudySetId) pendingOpenId = id else onOpenStudySet(id)
  }

  fun requestNew() {
    if (hasUnsavedChanges) confirmNew = true else onNewStudySet()
  }

  Column(modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text("Saved study sets", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
        Text(
          "${studySets.size} ${if (studySets.size == 1) "set" else "sets"} stored on this phone",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          style = MaterialTheme.typography.bodyMedium,
        )
      }
      Button(onClick = ::requestNew) {
        Icon(Icons.Rounded.Add, contentDescription = null)
        Spacer(Modifier.size(8.dp))
        Text("New")
      }
    }

    message?.let {
      Text(
        it,
        modifier = Modifier.padding(top = 10.dp),
        color = MaterialTheme.colorScheme.primary,
        style = MaterialTheme.typography.labelMedium,
      )
    }

    when {
      isLoading -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          CircularProgressIndicator()
        }
      }

      studySets.isEmpty() -> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
          Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
              Icons.Rounded.CollectionsBookmark,
              contentDescription = null,
              modifier = Modifier.size(44.dp),
              tint = MaterialTheme.colorScheme.primary,
            )
            Text("No saved study sets", style = MaterialTheme.typography.titleMedium)
          }
        }
      }

      else -> {
        LazyColumn(
          modifier = Modifier.fillMaxSize().padding(top = 14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
          items(studySets, key = StudySet::id) { studySet ->
            StudySetRow(
              studySet = studySet,
              selected = studySet.id == activeStudySetId,
              onOpen = { requestOpen(studySet.id) },
              onDelete = { pendingDeleteSet = studySet },
            )
          }
        }
      }
    }
  }

  pendingOpenId?.let { id ->
    UnsavedChangesDialog(
      onDismiss = { pendingOpenId = null },
      onDiscard = {
        pendingOpenId = null
        onOpenStudySet(id)
      },
    )
  }

  if (confirmNew) {
    UnsavedChangesDialog(
      onDismiss = { confirmNew = false },
      onDiscard = {
        confirmNew = false
        onNewStudySet()
      },
    )
  }

  pendingDeleteSet?.let { studySet ->
    AlertDialog(
      onDismissRequest = { pendingDeleteSet = null },
      title = { Text("Delete study set?") },
      text = { Text("${studySet.title} and its saved cards will be removed from this phone.") },
      dismissButton = { TextButton(onClick = { pendingDeleteSet = null }) { Text("Cancel") } },
      confirmButton = {
        TextButton(
          onClick = {
            pendingDeleteSet = null
            onDeleteStudySet(studySet.id)
          },
        ) {
          Text("Delete", color = MaterialTheme.colorScheme.error)
        }
      },
    )
  }
}

@Composable
private fun StudySetRow(
  studySet: StudySet,
  selected: Boolean,
  onOpen: () -> Unit,
  onDelete: () -> Unit,
) {
  Surface(
    onClick = onOpen,
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(8.dp),
    color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 6.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          studySet.title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          "${studySet.studyText.length} characters  |  ${studySet.flashcards.size} cards",
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
          "Updated ${DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(Date(studySet.updatedAt))}",
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      IconButton(onClick = onDelete) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete ${studySet.title}")
      }
      Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Open ${studySet.title}")
    }
  }
}

@Composable
private fun UnsavedChangesDialog(onDismiss: () -> Unit, onDiscard: () -> Unit) {
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("Discard unsaved changes?") },
    text = { Text("Save the current study set first if you want to keep these changes.") },
    dismissButton = { TextButton(onClick = onDismiss) { Text("Keep editing") } },
    confirmButton = { TextButton(onClick = onDiscard) { Text("Discard") } },
  )
}
