package com.example.hm_third_count.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hm_third_count.presentation.settings.ProfileView

private val ProfilePalette = listOf(
    "#4FC3F7", "#EF5350", "#66BB6A", "#FFA726",
    "#AB47BC", "#26C6DA", "#FFCA28", "#8D6E63"
)

private fun parseHex(hex: String): Color = runCatching {
    Color(android.graphics.Color.parseColor(hex))
}.getOrDefault(Color.Gray)

/**
 * Универсальный диалог управления профилями: список с активным маркером, переключение,
 * создание, переименование, удаление.
 */
@Composable
fun ProfilePickerDialog(
    profiles: List<ProfileView>,
    activeId: Long?,
    onSwitch: (Long) -> Unit,
    onCreate: (name: String, colorHex: String) -> Unit,
    onRename: (id: Long, newName: String) -> Unit,
    onDelete: (Long) -> Unit,
    onDismiss: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<ProfileView?>(null) }
    var deleteTarget by remember { mutableStateOf<ProfileView?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profiles") },
        text = {
            Column {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileRow(
                            profile = profile,
                            isActive = profile.id == activeId,
                            onSwitch = { onSwitch(profile.id) },
                            onRename = { renameTarget = profile },
                            onDelete = { deleteTarget = profile },
                            canDelete = profiles.size > 1
                        )
                    }
                    item {
                        TextButton(
                            onClick = { showCreate = true },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("+ New profile") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )

    if (showCreate) {
        ProfileCreateOrRenameDialog(
            title = "New profile",
            initialName = "",
            initialColor = ProfilePalette[0],
            onDismiss = { showCreate = false },
            onConfirm = { name, color ->
                onCreate(name, color)
                showCreate = false
            }
        )
    }

    renameTarget?.let { target ->
        ProfileCreateOrRenameDialog(
            title = "Rename profile",
            initialName = target.name,
            initialColor = target.colorHex,
            showColorPicker = false,
            onDismiss = { renameTarget = null },
            onConfirm = { name, _ ->
                onRename(target.id, name)
                renameTarget = null
            }
        )
    }

    deleteTarget?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete profile?") },
            text = {
                Text(
                    "All data of \"${target.name}\" (visits, wishlist, notes, " +
                        "collections, presets, history) will be lost."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteTarget = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun ProfileRow(
    profile: ProfileView,
    isActive: Boolean,
    onSwitch: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    canDelete: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSwitch)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(parseHex(profile.colorHex)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile.name.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.size(12.dp))
        Text(
            text = profile.name,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
        )
        if (isActive) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = "Active",
                tint = MaterialTheme.colorScheme.primary
            )
        }
        IconButton(onClick = onRename) {
            Icon(Icons.Default.Edit, contentDescription = "Rename")
        }
        if (canDelete) {
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "Delete")
            }
        }
    }
}

@Composable
private fun ProfileCreateOrRenameDialog(
    title: String,
    initialName: String,
    initialColor: String,
    showColorPicker: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: (name: String, colorHex: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var color by remember { mutableStateOf(initialColor) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(24) },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showColorPicker) {
                    Text("Avatar color", style = MaterialTheme.typography.labelLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        ProfilePalette.forEach { hex ->
                            val selected = hex == color
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(parseHex(hex))
                                    .clickable { color = hex }
                            ) {
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.25f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = name.isNotBlank(), onClick = { onConfirm(name.trim(), color) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

/** Маленький аватар-чип для шапки экрана. */
@Composable
fun ProfileAvatarChip(
    profile: ProfileView?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(onClick = onClick, modifier = modifier) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(profile?.let { parseHex(it.colorHex) } ?: Color.Gray),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = profile?.name?.firstOrNull()?.uppercase() ?: "?",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
