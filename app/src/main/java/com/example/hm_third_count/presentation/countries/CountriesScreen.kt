package com.example.hm_third_count.presentation.countries

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.example.hm_third_count.data.model.Country
import com.example.hm_third_count.presentation.profile.ProfileAvatarChip
import com.example.hm_third_count.presentation.profile.ProfilePickerDialog
import com.example.hm_third_count.presentation.profile.ProfileSwitcherViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountriesScreen(
    uiState: CountriesUiState,
    onEvent: (CountriesEvent) -> Unit,
    onCountryClick: (String) -> Unit,
    onOpenRecent: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 16.dp, bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { onEvent(CountriesEvent.SearchQueryChanged(it)) },
                label = { Text("Search countries") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                modifier = Modifier.weight(1f),
                singleLine = true
            )
            IconButton(onClick = onOpenRecent) {
                Icon(Icons.Default.History, contentDescription = "Recently viewed")
            }
            ProfileSwitcherSlot()
        }
        
        RegionFilter(
            selectedRegion = uiState.selectedRegion,
            showFavoritesOnly = uiState.showFavoritesOnly,
            sortByNameAz = uiState.sortByNameAz,
            onRegionSelected = { onEvent(CountriesEvent.RegionSelected(it)) },
            onShowFavorites = { onEvent(CountriesEvent.ShowFavorites) },
            onToggleSortAz = { onEvent(CountriesEvent.ToggleSortAz) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        if (uiState.isBackgroundSyncing) {
            SyncBanner()
        }

        SavedFiltersRow(
            presets = uiState.savedFilters,
            canSaveCurrent = uiState.canSaveCurrentAsPreset,
            onApply = { onEvent(CountriesEvent.ApplyPreset(it)) },
            onDelete = { onEvent(CountriesEvent.DeletePreset(it)) },
            onSaveCurrent = { name -> onEvent(CountriesEvent.SaveCurrentAsPreset(name)) },
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Content
        when {
            uiState.isLoading -> {
                LoadingContent()
            }
            uiState.error != null -> {
                ErrorContent(
                    error = uiState.error,
                    onRetry = { onEvent(CountriesEvent.Retry) }
                )
            }
            uiState.isEmpty -> {
                EmptyContent()
            }
            else -> {
                CountriesList(
                    countries = uiState.countries,
                    favorites = uiState.favorites,
                    visitedCodes = uiState.visitedCodes,
                    showVisitedBadge = uiState.showVisitedBadge,
                    onCountryClick = onCountryClick,
                    onFavoriteClick = { onEvent(CountriesEvent.ToggleFavorite(it)) },
                    showFavoritesOnly = uiState.showFavoritesOnly
                )
            }
        }
    }
}

@Composable
private fun RegionFilter(
    selectedRegion: String,
    showFavoritesOnly: Boolean,
    sortByNameAz: Boolean,
    onRegionSelected: (String) -> Unit,
    onShowFavorites: () -> Unit,
    onToggleSortAz: () -> Unit,
    modifier: Modifier = Modifier
) {
    val regions = listOf("Africa", "Americas", "Asia", "Europe", "Oceania")

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        item {
            FilterChip(
                onClick = onToggleSortAz,
                label = { Text("A–Z") },
                selected = sortByNameAz
            )
        }
        item {
            FilterChip(
                onClick = onShowFavorites,
                label = { Text("Favourite") },
                selected = showFavoritesOnly,
                leadingIcon = if (showFavoritesOnly) {
                    { Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
        
        items(regions.size) { index ->
            val region = regions[index]
            FilterChip(
                onClick = { onRegionSelected(region) },
                label = { Text(region) },
                selected = selectedRegion == region && !showFavoritesOnly
            )
        }
    }
}

/**
 * Самодостаточный аватар-чип с быстрым переключателем профиля.
 * Имеет собственную ViewModel — экран остаётся stateless относительно профилей.
 */
@Composable
private fun ProfileSwitcherSlot(viewModel: ProfileSwitcherViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showPicker by remember { mutableStateOf(false) }

    ProfileAvatarChip(
        profile = state.activeProfile,
        onClick = { showPicker = true }
    )

    if (showPicker) {
        ProfilePickerDialog(
            profiles = state.profiles,
            activeId = state.activeProfileId,
            onSwitch = {
                viewModel.switch(it)
            },
            onCreate = { name, color -> viewModel.create(name, color) },
            onRename = { id, name -> viewModel.rename(id, name) },
            onDelete = { viewModel.delete(it) },
            onDismiss = { showPicker = false }
        )
    }
}

@Composable
private fun SyncBanner() {
    Surface(
        color = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(14.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            Text(
                "Updating in background…",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun SavedFiltersRow(
    presets: List<SavedFilterView>,
    canSaveCurrent: Boolean,
    onApply: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onSaveCurrent: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    if (presets.isEmpty() && !canSaveCurrent) return

    var showSaveDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<SavedFilterView?>(null) }

    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(bottom = 8.dp)
    ) {
        if (canSaveCurrent) {
            item {
                AssistChip(
                    onClick = { showSaveDialog = true },
                    leadingIcon = {
                        Icon(
                            Icons.Default.BookmarkAdd,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    },
                    label = { Text("Save current") }
                )
            }
        }
        items(presets, key = { it.id }) { preset ->
            InputChip(
                selected = false,
                onClick = { onApply(preset.id) },
                label = { Text(preset.name) },
                trailingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Delete preset",
                        modifier = Modifier
                            .size(16.dp)
                            .clickable { deleteCandidate = preset }
                    )
                }
            )
        }
    }

    if (showSaveDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save preset") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it.take(30) },
                    label = { Text("Preset name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    enabled = name.isNotBlank(),
                    onClick = {
                        onSaveCurrent(name.trim())
                        showSaveDialog = false
                    }
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") }
            }
        )
    }

    deleteCandidate?.let { target ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete preset?") },
            text = { Text("\"${target.name}\" will be removed.") },
            confirmButton = {
                TextButton(onClick = {
                    onDelete(target.id)
                    deleteCandidate = null
                }) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") }
            }
        )
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
            Text("Loading countries...")
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    val isNetworkError = error.contains("resolve", ignoreCase = true) ||
        error.contains("host", ignoreCase = true) ||
        error.contains("connect", ignoreCase = true) ||
        error.contains("timeout", ignoreCase = true) ||
        error.contains("network", ignoreCase = true) ||
        error.contains("internet", ignoreCase = true)

    val title = if (isNetworkError) "No internet connection" else "Something went wrong"
    val message = if (isNetworkError) {
        "Check your network and try again."
    } else {
        "We couldn't load countries. Please try again."
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
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            Spacer(Modifier.size(4.dp))
            Button(onClick = onRetry) {
                Text("Try again")
            }
        }
    }
}

@Composable
private fun EmptyContent() {
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
                imageVector = Icons.Default.SearchOff,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "No countries found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = "Try a different search or filter.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CountriesList(
    countries: List<Country>,
    favorites: Set<String>,
    visitedCodes: Set<String>,
    showVisitedBadge: Boolean,
    onCountryClick: (String) -> Unit,
    onFavoriteClick: (Country) -> Unit,
    showFavoritesOnly: Boolean = false
) {
    if (showFavoritesOnly && countries.isEmpty()) {
        // Показываем специальное сообщение для пустого избранного
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "No favorite countries yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Add countries to favorites by tapping the heart icon",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(countries) { country ->
                CountryItem(
                    country = country,
                    isFavorite = favorites.contains(country.code),
                    isVisited = showVisitedBadge && visitedCodes.contains(country.code),
                    onClick = { onCountryClick(country.code) },
                    onFavoriteClick = { onFavoriteClick(country) }
                )
            }
        }
    }
}

@Composable
private fun CountryItem(
    country: Country,
    isFavorite: Boolean,
    isVisited: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = country.flags.png,
                contentDescription = "Flag of ${country.name.common}",
                modifier = Modifier
                    .size(76.dp, 52.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = country.name.common,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (isVisited) {
                        Spacer(Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Visited",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = country.capital?.firstOrNull() ?: "No capital",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${country.region} · ${formatPopulation(country.population)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
private fun formatPopulation(n: Long): String = when {
    n >= 1_000_000_000L -> "%.1fB people".format(n / 1_000_000_000.0)
    n >= 1_000_000L -> "%.1fM people".format(n / 1_000_000.0)
    n >= 1_000L -> "%.0fK people".format(n / 1_000.0)
    else -> "$n people"
}
