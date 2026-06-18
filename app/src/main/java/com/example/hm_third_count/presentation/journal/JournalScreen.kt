package com.example.hm_third_count.presentation.journal

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalContentColor
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.hm_third_count.data.model.Country
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val isoDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JournalScreen(
    uiState: JournalUiState,
    onEvent: (JournalEvent) -> Unit,
    onCountryClick: (String) -> Unit
) {
    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf("Visited", "Wishlist", "Notes")

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Journal") },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            JournalStatsCard(stats = uiState.stats)
            TabRow(selectedTabIndex = tab) {
                tabs.forEachIndexed { index, label ->
                    val count = when (index) {
                        0 -> uiState.stats.visitedCount
                        1 -> uiState.stats.wishlistCount
                        else -> uiState.stats.notesCount
                    }
                    Tab(
                        selected = tab == index,
                        onClick = { tab = index },
                        text = { Text(if (count > 0) "$label ($count)" else label) }
                    )
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    uiState.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) {
                        CircularProgressIndicator()
                    }
                    else -> when (tab) {
                        0 -> VisitedTab(uiState.visited, onCountryClick) {
                            onEvent(JournalEvent.RemoveVisit(it))
                        }
                        1 -> WishlistTab(uiState.wishlist, onCountryClick) {
                            onEvent(JournalEvent.RemoveWishlist(it))
                        }
                        2 -> NotesTab(uiState.notes, onCountryClick) {
                            onEvent(JournalEvent.RemoveNote(it))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JournalStatsCard(stats: JournalStats) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 18.dp, horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            Stat(stats.visitedCount.toString(), "Visited")
            StatDivider()
            Stat(stats.regionsCount.toString(), "Regions")
            StatDivider()
            Stat(
                if (stats.visitedCount > 0) String.format(Locale.US, "%.1f", stats.averageRating)
                else "—",
                "Avg rating"
            )
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = LocalContentColor.current.copy(alpha = 0.72f)
        )
    }
}

@Composable
private fun StatDivider() {
    Box(
        modifier = Modifier
            .height(36.dp)
            .padding(horizontal = 4.dp)
            .background(
                color = LocalContentColor.current.copy(alpha = 0.18f),
                shape = RoundedCornerShape(50)
            )
            .width(1.dp)
    )
}

@Composable
private fun VisitedTab(
    items: List<VisitedItem>,
    onCountryClick: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    if (items.isEmpty()) {
        EmptyMessage("No visited countries yet. Open a country and mark it as visited.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.country.code }) { item ->
            JournalRow(
                country = item.country,
                subtitle = "Visited ${isoDate.format(Date(item.visitedAt))} • ${item.rating}★" +
                    (item.note?.takeIf { it.isNotBlank() }?.let { " • \"$it\"" } ?: ""),
                onClick = { onCountryClick(item.country.code) },
                onRemove = { onRemove(item.country.code) }
            )
        }
    }
}

@Composable
private fun WishlistTab(
    items: List<WishlistItem>,
    onCountryClick: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    if (items.isEmpty()) {
        EmptyMessage("Wishlist is empty. Add countries you want to visit.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.country.code }) { item ->
            JournalRow(
                country = item.country,
                subtitle = "Priority ${item.priority}/5" +
                    (item.plannedDate?.let { " • planned ${isoDate.format(Date(it))}" } ?: ""),
                onClick = { onCountryClick(item.country.code) },
                onRemove = { onRemove(item.country.code) }
            )
        }
    }
}

@Composable
private fun NotesTab(
    items: List<NoteItem>,
    onCountryClick: (String) -> Unit,
    onRemove: (String) -> Unit
) {
    if (items.isEmpty()) {
        EmptyMessage("No notes yet. Open a country and add a personal note.")
        return
    }
    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.country.code }) { item ->
            JournalRow(
                country = item.country,
                subtitle = item.text,
                onClick = { onCountryClick(item.country.code) },
                onRemove = { onRemove(item.country.code) }
            )
        }
    }
}

@Composable
private fun JournalRow(
    country: Country,
    subtitle: String,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = country.flags.png,
                contentDescription = null,
                modifier = Modifier.size(48.dp, 32.dp),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(country.name.common, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onRemove) {
                Icon(Icons.Default.Close, contentDescription = "Remove")
            }
        }
    }
}

@Composable
private fun EmptyMessage(text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Default.TravelExplore,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
    }
}
