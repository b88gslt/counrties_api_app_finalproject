package com.example.hm_third_count.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.navigation
import androidx.navigation.compose.rememberNavController
import androidx.navigation.NavType
import androidx.navigation.navArgument
import com.example.hm_third_count.presentation.collections.CollectionDetailScreen
import com.example.hm_third_count.presentation.collections.CollectionDetailViewModel
import com.example.hm_third_count.presentation.collections.CollectionsScreen
import com.example.hm_third_count.presentation.collections.CollectionsViewModel
import com.example.hm_third_count.presentation.countries.CountriesScreen
import com.example.hm_third_count.presentation.countries.CountriesViewModel
import com.example.hm_third_count.presentation.detail.CountryDetailScreen
import com.example.hm_third_count.presentation.detail.CountryDetailViewModel
import com.example.hm_third_count.presentation.journal.JournalScreen
import com.example.hm_third_count.presentation.journal.JournalViewModel
import com.example.hm_third_count.presentation.recent.RecentScreen
import com.example.hm_third_count.presentation.recent.RecentViewModel
import com.example.hm_third_count.presentation.settings.SettingsScreen
import com.example.hm_third_count.presentation.settings.SettingsViewModel

private object Routes {
    const val COUNTRIES_LIST = "countries/list"
    const val COUNTRIES_RECENT = "countries/recent"
    const val COUNTRY_DETAIL = "countries/detail/{countryCode}"
    fun detail(code: String) = "countries/detail/$code"

    const val JOURNAL_HOME = "journal/home"
    const val COLLECTIONS_HOME = "collections/home"
    const val COLLECTION_DETAIL = "collections/{collectionId}"
    fun collectionDetail(id: Long) = "collections/$id"
    const val SETTINGS_HOME = "settings/home"
}

@Composable
fun CountriesNavigation(navController: NavHostController = rememberNavController()) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                TopLevelDestination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { padding ->
        AppNavHost(
            navController = navController,
            padding = padding
        )
    }
}

@Composable
private fun AppNavHost(
    navController: NavHostController,
    padding: PaddingValues
) {
    NavHost(
        navController = navController,
        startDestination = TopLevelDestination.COUNTRIES.route,
        modifier = Modifier.padding(padding)
    ) {
        countriesGraph(navController)
        navigation(
            route = TopLevelDestination.JOURNAL.route,
            startDestination = Routes.JOURNAL_HOME
        ) {
            composable(Routes.JOURNAL_HOME) {
                val viewModel: JournalViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                JournalScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onCountryClick = { code -> navController.navigate(Routes.detail(code)) }
                )
            }
        }
        navigation(
            route = TopLevelDestination.COLLECTIONS.route,
            startDestination = Routes.COLLECTIONS_HOME
        ) {
            composable(Routes.COLLECTIONS_HOME) {
                val viewModel: CollectionsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                CollectionsScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onCollectionClick = { id -> navController.navigate(Routes.collectionDetail(id)) }
                )
            }
            composable(
                route = Routes.COLLECTION_DETAIL,
                arguments = listOf(navArgument("collectionId") { type = NavType.LongType })
            ) {
                val viewModel: CollectionDetailViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                CollectionDetailScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent,
                    onCountryClick = { code -> navController.navigate(Routes.detail(code)) },
                    onBackClick = { navController.popBackStack() },
                    onDeleted = { navController.popBackStack() }
                )
            }
        }
        navigation(
            route = TopLevelDestination.SETTINGS.route,
            startDestination = Routes.SETTINGS_HOME
        ) {
            composable(Routes.SETTINGS_HOME) {
                val viewModel: SettingsViewModel = hiltViewModel()
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()
                SettingsScreen(
                    uiState = uiState,
                    onEvent = viewModel::onEvent
                )
            }
        }
        // Country detail — top-level, доступен из любой вкладки. Back возвращает на источник.
        composable(Routes.COUNTRY_DETAIL) {
            val viewModel: CountryDetailViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            CountryDetailScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}

private fun androidx.navigation.NavGraphBuilder.countriesGraph(navController: NavHostController) {
    navigation(
        route = TopLevelDestination.COUNTRIES.route,
        startDestination = Routes.COUNTRIES_LIST
    ) {
        composable(Routes.COUNTRIES_LIST) {
            val viewModel: CountriesViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            CountriesScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onCountryClick = { code -> navController.navigate(Routes.detail(code)) },
                onOpenRecent = { navController.navigate(Routes.COUNTRIES_RECENT) }
            )
        }
        composable(Routes.COUNTRIES_RECENT) {
            val viewModel: RecentViewModel = hiltViewModel()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            RecentScreen(
                uiState = uiState,
                onEvent = viewModel::onEvent,
                onCountryClick = { code -> navController.navigate(Routes.detail(code)) },
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
