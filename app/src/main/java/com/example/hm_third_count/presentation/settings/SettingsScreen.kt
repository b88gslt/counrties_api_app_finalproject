package com.example.hm_third_count.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.example.hm_third_count.presentation.profile.ProfilePickerDialog
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.hm_third_count.data.local.AreaUnit
import com.example.hm_third_count.data.local.ThemeMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onEvent: (SettingsEvent) -> Unit
) {
    var showProfilePicker by remember { mutableStateOf(false) }

    if (showProfilePicker) {
        ProfilePickerDialog(
            profiles = uiState.profiles,
            activeId = uiState.activeProfileId,
            onSwitch = {
                onEvent(SettingsEvent.SwitchProfile(it))
            },
            onCreate = { name, color ->
                onEvent(SettingsEvent.CreateProfile(name, color))
            },
            onRename = { id, name ->
                onEvent(SettingsEvent.RenameProfile(id, name))
            },
            onDelete = {
                onEvent(SettingsEvent.DeleteProfile(it))
            },
            onDismiss = { showProfilePicker = false }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Settings") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsCard(title = "Profile") {
                val active = uiState.profiles.firstOrNull { it.id == uiState.activeProfileId }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    androidx.compose.foundation.layout.Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                runCatching { Color(android.graphics.Color.parseColor(active?.colorHex ?: "#888888")) }
                                    .getOrDefault(Color.Gray)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = active?.name?.firstOrNull()?.uppercase() ?: "?",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(Modifier.size(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = active?.name ?: "—",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "${uiState.profiles.size} profile(s)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    OutlinedButton(onClick = { showProfilePicker = true }) {
                        Text("Manage")
                    }
                }
            }

            SettingsCard(title = "Appearance") {
                Text("Theme", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        FilterChip(
                            selected = uiState.theme == mode,
                            onClick = { onEvent(SettingsEvent.SetTheme(mode)) },
                            label = { Text(mode.name.lowercase().replaceFirstChar { it.uppercase() }) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Area unit", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    AreaUnit.entries.forEach { unit ->
                        FilterChip(
                            selected = uiState.areaUnit == unit,
                            onClick = { onEvent(SettingsEvent.SetAreaUnit(unit)) },
                            label = { Text(if (unit == AreaUnit.KM2) "km²" else "mi²") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Show visited badge", modifier = Modifier.weight(1f))
                    Switch(
                        checked = uiState.showVisitedBadge,
                        onCheckedChange = { onEvent(SettingsEvent.SetShowVisitedBadge(it)) }
                    )
                }
            }

            SettingsCard(title = "Offline & sync") {
                Text("Cache TTL", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(6, 12, 24, 72).forEach { hours ->
                        FilterChip(
                            selected = uiState.cacheTtlHours == hours,
                            onClick = { onEvent(SettingsEvent.SetCacheTtl(hours)) },
                            label = { Text("${hours}h") }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Background refresh", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0, 6, 12, 24).forEach { hours ->
                        FilterChip(
                            selected = uiState.backgroundRefreshHours == hours,
                            onClick = { onEvent(SettingsEvent.SetBackgroundRefresh(hours)) },
                            label = { Text(if (hours == 0) "Off" else "${hours}h") }
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                LastSyncInfo(lastSyncAt = uiState.lastSyncAt, running = uiState.syncRunning)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onEvent(SettingsEvent.SyncNow) },
                        enabled = !uiState.syncRunning
                    ) { Text("Sync now") }
                    OutlinedButton(
                        onClick = { onEvent(SettingsEvent.DownloadAllCountries) },
                        enabled = !uiState.syncRunning
                    ) { Text("Download all") }
                }
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = title.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.SemiBold
            )
            content()
        }
    }
}

@Composable
private fun LastSyncInfo(lastSyncAt: Long, running: Boolean) {
    val now = remember { System.currentTimeMillis() }
    val label = when {
        running -> "Syncing now…"
        lastSyncAt == 0L -> "Never synced"
        else -> "Last sync ${formatAge(now - lastSyncAt)}"
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (running) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.size(8.dp))
        }
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

private fun formatAge(deltaMs: Long): String {
    if (deltaMs < 0L) return "just now"
    val seconds = deltaMs / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return when {
        seconds < 60 -> "just now"
        minutes < 60 -> "${minutes}m ago"
        hours < 24 -> "${hours}h ago"
        else -> "${days}d ago"
    }
}
