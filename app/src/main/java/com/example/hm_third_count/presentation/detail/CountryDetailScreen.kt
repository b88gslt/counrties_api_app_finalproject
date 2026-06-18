package com.example.hm_third_count.presentation.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.hm_third_count.data.local.AreaUnit
import com.example.hm_third_count.data.model.Country

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountryDetailScreen(
    uiState: CountryDetailUiState,
    onEvent: (CountryDetailEvent) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showVisitDialog by remember { mutableStateOf(false) }
    var showWishlistDialog by remember { mutableStateOf(false) }
    var showNoteDialog by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }

    if (showVisitDialog) {
        MarkVisitedDialog(
            initial = uiState.visit,
            onDismiss = { showVisitDialog = false },
            onSave = { date, rating, note ->
                onEvent(CountryDetailEvent.SaveVisit(date, rating, note))
                showVisitDialog = false
            },
            onDelete = {
                onEvent(CountryDetailEvent.DeleteVisit)
                showVisitDialog = false
            }
        )
    }
    if (showWishlistDialog) {
        WishlistDialog(
            initial = uiState.wishlist,
            onDismiss = { showWishlistDialog = false },
            onSave = { priority, planned ->
                onEvent(CountryDetailEvent.SaveWishlist(priority, planned))
                showWishlistDialog = false
            },
            onDelete = {
                onEvent(CountryDetailEvent.DeleteWishlist)
                showWishlistDialog = false
            }
        )
    }
    if (showNoteDialog) {
        NoteDialog(
            initialText = uiState.note,
            onDismiss = { showNoteDialog = false },
            onSave = {
                onEvent(CountryDetailEvent.SaveNote(it))
                showNoteDialog = false
            },
            onDelete = {
                onEvent(CountryDetailEvent.DeleteNote)
                showNoteDialog = false
            }
        )
    }
    if (showCollectionPicker) {
        CollectionPickerDialog(
            allCollections = uiState.allCollections,
            selectedIds = uiState.memberOfCollections,
            onDismiss = { showCollectionPicker = false },
            onSave = {
                onEvent(CountryDetailEvent.SetCollections(it))
                showCollectionPicker = false
            },
            onCreateNew = { name, color ->
                onEvent(CountryDetailEvent.CreateAndAddCollection(name, color))
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text(uiState.country?.name?.common ?: "Country Details") },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            actions = {
                if (uiState.country != null) {
                    IconButton(
                        onClick = { onEvent(CountryDetailEvent.ManualRefresh) },
                        enabled = !uiState.isRefreshing
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh from network"
                        )
                    }
                    IconButton(onClick = { onEvent(CountryDetailEvent.ToggleFavorite) }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        )

        if (uiState.country != null) {
            CacheStatusBanner(
                fetchedAt = uiState.fetchedAt,
                isStale = uiState.isStale,
                isRefreshing = uiState.isRefreshing
            )
        }

        when {
            uiState.isLoading -> {
                LoadingContent()
            }
            uiState.error != null -> {
                ErrorContent(
                    error = uiState.error,
                    onRetry = { onEvent(CountryDetailEvent.Retry) }
                )
            }
            uiState.country != null -> {
                CountryDetailContent(
                    country = uiState.country,
                    uiState = uiState,
                    onVisitClick = { showVisitDialog = true },
                    onWishlistClick = { showWishlistDialog = true },
                    onNoteClick = { showNoteDialog = true },
                    onCollectionsClick = { showCollectionPicker = true }
                )
            }
        }
    }
}

@Composable
private fun CacheStatusBanner(
    fetchedAt: Long?,
    isStale: Boolean,
    isRefreshing: Boolean
) {
    val now = remember { System.currentTimeMillis() }
    val ageLabel = fetchedAt?.let { formatAge(now - it) } ?: "—"
    val containerColor = when {
        isRefreshing -> MaterialTheme.colorScheme.tertiaryContainer
        isStale -> MaterialTheme.colorScheme.errorContainer
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val contentColor = when {
        isRefreshing -> MaterialTheme.colorScheme.onTertiaryContainer
        isStale -> MaterialTheme.colorScheme.onErrorContainer
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(color = containerColor, contentColor = contentColor) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (isRefreshing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                    color = contentColor
                )
                Text(
                    text = "Updating…",
                    style = MaterialTheme.typography.bodySmall
                )
            } else if (isStale) {
                Icon(
                    imageVector = Icons.Default.CloudOff,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = "Cached data — may be outdated (updated $ageLabel)",
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    text = "Updated $ageLabel",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
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

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator()
            Text("Loading country details...")
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    val isNetwork = error.contains("resolve", ignoreCase = true) ||
        error.contains("host", ignoreCase = true) ||
        error.contains("connect", ignoreCase = true) ||
        error.contains("timeout", ignoreCase = true) ||
        error.contains("network", ignoreCase = true) ||
        error.contains("internet", ignoreCase = true)

    val title = if (isNetwork) "No internet connection" else "Couldn't load country"
    val subtitle = if (isNetwork) {
        "We can't reach the server right now."
    } else {
        "Something went wrong. Please try again."
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.CloudOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.size(4.dp))
            Button(onClick = onRetry) { Text("Try again") }
        }
    }
}

@Composable
private fun CountryDetailContent(
    country: Country,
    uiState: CountryDetailUiState,
    onVisitClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onNoteClick: () -> Unit,
    onCollectionsClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Flag
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            AsyncImage(
                model = country.flags.png,
                contentDescription = "Flag of ${country.name.common}",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentScale = ContentScale.Crop
            )
        }

        // Travel Journal section
        JournalSection(
            uiState = uiState,
            onVisitClick = onVisitClick,
            onWishlistClick = onWishlistClick,
            onNoteClick = onNoteClick,
            onCollectionsClick = onCollectionsClick
        )

        // Basic Information
        InfoSection(title = "Basic Information") {
            InfoItem("Official Name", country.name.official)
            InfoItem("Common Name", country.name.common)
            InfoItem("Capital", country.capital?.joinToString(", ") ?: "No capital")
            InfoItem("Region", country.region)
            country.subregion?.let { InfoItem("Subregion", it) }
        }
        
        // Demographics
        InfoSection(title = "Demographics") {
            InfoItem("Population", String.format("%,d", country.population))
            country.area?.let { areaKm2 ->
                val (value, label) = when (uiState.areaUnit) {
                    AreaUnit.KM2 -> areaKm2 to "km²"
                    AreaUnit.MI2 -> (areaKm2 * 0.3861022) to "mi²"
                }
                InfoItem("Area", "${String.format("%,.0f", value)} $label")
            }
        }
        
        // Languages
        country.languages?.let { languages ->
            InfoSection(title = "Languages") {
                languages.values.forEach { language ->
                    InfoItem("", language)
                }
            }
        }
        
        // Currencies
        country.currencies?.let { currencies ->
            InfoSection(title = "Currencies") {
                currencies.values.forEach { currency ->
                    InfoItem(
                        currency.name,
                        currency.symbol?.let { "Symbol: $it" } ?: ""
                    )
                }
            }
        }
        
        // Timezones
        country.timezones?.let { timezones ->
            InfoSection(title = "Timezones") {
                timezones.forEach { timezone ->
                    InfoItem("", timezone)
                }
            }
        }
        
        // Borders
        country.borders?.let { borders ->
            if (borders.isNotEmpty()) {
                InfoSection(title = "Border Countries") {
                    InfoItem("", borders.joinToString(", "))
                }
            }
        }
    }
}

@Composable
private fun JournalSection(
    uiState: CountryDetailUiState,
    onVisitClick: () -> Unit,
    onWishlistClick: () -> Unit,
    onNoteClick: () -> Unit,
    onCollectionsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "My travel",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            JournalChipRow(
                visited = uiState.visit != null,
                inWishlist = uiState.wishlist != null,
                hasNote = !uiState.note.isNullOrBlank(),
                collectionsCount = uiState.memberOfCollections.size,
                onVisitedClick = onVisitClick,
                onWishlistClick = onWishlistClick,
                onNoteClick = onNoteClick,
                onCollectionsClick = onCollectionsClick,
                modifier = Modifier.fillMaxWidth()
            )
            uiState.visit?.let { v ->
                Text(
                    text = "Visited • rated ${v.rating}/5${v.note?.let { " • \"$it\"" } ?: ""}",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            uiState.wishlist?.let { w ->
                Text(
                    text = "In wishlist • priority ${w.priority}/5",
                    style = MaterialTheme.typography.bodySmall
                )
            }
            uiState.note?.takeIf { it.isNotBlank() }?.let { txt ->
                Text(
                    text = "Note: $txt",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Start
                )
            }
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            content()
        }
    }
}

@Composable
private fun InfoItem(
    label: String,
    value: String
) {
    if (value.isNotBlank()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (label.isNotBlank()) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.weight(1f)
                )
            } else {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}