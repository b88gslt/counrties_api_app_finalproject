package com.example.hm_third_count.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.outlined.Description
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val prettyDate = SimpleDateFormat("d MMM yyyy", Locale.US)

/**
 * Кликабельное поле «дата» с подзаголовком. По нажатию открывает Material 3 DatePicker.
 * Возвращает выбранный timestamp через onChange.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateField(
    label: String,
    value: Long?,
    placeholder: String,
    onChange: (Long?) -> Unit,
    allowClear: Boolean = false
) {
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
            .clickable { showPicker = true }
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.size(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = value?.let { prettyDate.format(Date(it)) } ?: placeholder,
                style = MaterialTheme.typography.bodyLarge,
                color = if (value != null) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            if (allowClear && value != null) {
                TextButton(onClick = { onChange(null) }) { Text("Clear") }
            }
        }
    }

    if (showPicker) {
        val state = rememberDatePickerState(initialSelectedDateMillis = value)
        DatePickerDialog(
            onDismissRequest = { showPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    onChange(state.selectedDateMillis)
                    showPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }
}

@Composable
fun MarkVisitedDialog(
    initial: VisitView?,
    onDismiss: () -> Unit,
    onSave: (visitedAt: Long, rating: Int, note: String?) -> Unit,
    onDelete: () -> Unit
) {
    var visitedAt by remember {
        mutableStateOf(initial?.visitedAt ?: System.currentTimeMillis())
    }
    var rating by remember { mutableIntStateOf(initial?.rating ?: 5) }
    var note by remember { mutableStateOf(initial?.note.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Mark as visited" else "Edit visit") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                DateField(
                    label = "Date",
                    value = visitedAt,
                    placeholder = "Pick a date",
                    onChange = { it?.let { v -> visitedAt = v } }
                )
                Text("Rating", style = MaterialTheme.typography.labelLarge)
                StarRow(rating = rating, onRatingChange = { rating = it })
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it },
                    label = { Text("Note (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSave(visitedAt, rating, note.takeIf { it.isNotBlank() })
            }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun WishlistDialog(
    initial: WishlistView?,
    onDismiss: () -> Unit,
    onSave: (priority: Int, plannedDate: Long?) -> Unit,
    onDelete: () -> Unit
) {
    var priority by remember { mutableIntStateOf(initial?.priority ?: 3) }
    var plannedDate by remember { mutableStateOf<Long?>(initial?.plannedDate) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initial == null) "Add to wishlist" else "Edit wishlist entry") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Priority", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    (1..5).forEach { p ->
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(p.toString()) }
                        )
                    }
                }
                DateField(
                    label = "Planned date (optional)",
                    value = plannedDate,
                    placeholder = "Pick a date",
                    onChange = { plannedDate = it },
                    allowClear = true
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(priority, plannedDate) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (initial != null) {
                    TextButton(onClick = onDelete) { Text("Remove") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
fun NoteDialog(
    initialText: String?,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    onDelete: () -> Unit
) {
    var text by remember { mutableStateOf(initialText.orEmpty()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (initialText == null) "Add note" else "Edit note") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                label = { Text("Your note") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 8
            )
        },
        confirmButton = {
            TextButton(onClick = { onSave(text) }) { Text("Save") }
        },
        dismissButton = {
            Row {
                if (!initialText.isNullOrBlank()) {
                    TextButton(onClick = onDelete) { Text("Delete") }
                }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

@Composable
private fun StarRow(rating: Int, onRatingChange: (Int) -> Unit) {
    Row {
        (1..5).forEach { star ->
            IconButton(onClick = { onRatingChange(star) }) {
                Icon(
                    imageVector = if (star <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = "$star",
                    tint = if (star <= rating) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun JournalChipRow(
    visited: Boolean,
    inWishlist: Boolean,
    hasNote: Boolean,
    collectionsCount: Int,
    onVisitedClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onNoteClick: () -> Unit,
    onCollectionsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        JournalActionRow(
            icon = Icons.Filled.Place,
            title = "Mark as visited",
            subtitle = if (visited) "Marked ✓" else "Not visited yet",
            active = visited,
            onClick = onVisitedClick
        )
        JournalActionRow(
            icon = Icons.Filled.Star,
            title = "Add to wishlist",
            subtitle = if (inWishlist) "In your wishlist ★" else "Not in wishlist",
            active = inWishlist,
            onClick = onWishlistClick
        )
        JournalActionRow(
            icon = Icons.Outlined.Description,
            title = "Personal note",
            subtitle = if (hasNote) "Saved" else "No note",
            active = hasNote,
            onClick = onNoteClick
        )
        JournalActionRow(
            icon = Icons.Filled.Bookmark,
            title = "Add to collection",
            subtitle = if (collectionsCount > 0) {
                "In $collectionsCount collection${if (collectionsCount > 1) "s" else ""}"
            } else "Not in any collection",
            active = collectionsCount > 0,
            onClick = onCollectionsClick
        )
    }
}

@Composable
private fun JournalActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    active: Boolean,
    onClick: () -> Unit
) {
    val accent = if (active) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant
    androidx.compose.foundation.layout.Row(
        modifier = androidx.compose.ui.Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = androidx.compose.ui.Modifier.size(22.dp)
        )
        Spacer(androidx.compose.ui.Modifier.size(14.dp))
        Column(modifier = androidx.compose.ui.Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = accent
            )
        }
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun CollectionPickerDialog(
    allCollections: List<CollectionSummaryView>,
    selectedIds: Set<Long>,
    onDismiss: () -> Unit,
    onSave: (Set<Long>) -> Unit,
    onCreateNew: (name: String, colorHex: String) -> Unit
) {
    var pending by remember(selectedIds) { mutableStateOf(selectedIds) }
    var showCreate by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add to collection") },
        text = {
            Column {
                if (allCollections.isEmpty()) {
                    Text(
                        "You don't have any collections yet.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                }
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(allCollections, key = { it.id }) { c ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    pending = if (c.id in pending) pending - c.id
                                    else pending + c.id
                                },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = c.id in pending,
                                onCheckedChange = {
                                    pending = if (it) pending + c.id else pending - c.id
                                }
                            )
                            Box(
                                modifier = Modifier
                                    .size(16.dp)
                                    .clip(CircleShape)
                                    .background(
                                        runCatching { androidx.compose.ui.graphics.Color(android.graphics.Color.parseColor(c.colorHex)) }
                                            .getOrDefault(androidx.compose.ui.graphics.Color.Gray)
                                    )
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(c.name)
                        }
                    }
                    item {
                        TextButton(
                            onClick = { showCreate = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("+ Create new collection") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(pending) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )

    if (showCreate) {
        com.example.hm_third_count.presentation.collections.CreateOrRenameDialog(
            initialName = "",
            initialColor = "#4FC3F7",
            title = "New collection",
            onDismiss = { showCreate = false },
            onConfirm = { name, color ->
                onCreateNew(name, color)
                showCreate = false
            }
        )
    }
}
