package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoFixHigh
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.outlined.AutoFixHigh
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Store
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.Artisan
import com.example.ui.screens.AiDiagnosticScreen
import com.example.ui.screens.AssetLogbookScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MarketplaceScreen
import com.example.ui.screens.ProfileScreen
import com.example.ui.viewmodel.SiyanetaViewModel

data class NavItem(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

val navItems = listOf(
    NavItem("الرئيسية", Icons.Filled.Home, Icons.Outlined.Home, "nav_home"),
    NavItem("التشخيص AI", Icons.Filled.AutoFixHigh, Icons.Outlined.AutoFixHigh, "nav_ai"),
    NavItem("سجل الأصول", Icons.Filled.DirectionsCar, Icons.Outlined.DirectionsCar, "nav_assets"),
    NavItem("سوق الحرفيين", Icons.Filled.Store, Icons.Outlined.Store, "nav_marketplace"),
    NavItem("حسابي", Icons.Filled.Person, Icons.Outlined.Person, "nav_profile")
)

@Composable
fun MainAppContent(
    viewModel: SiyanetaViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedIndex by remember { mutableIntStateOf(0) }
    var marketplaceCategoryFilter by remember { mutableStateOf<String?>(null) }
    var marketplaceProblemDesc by remember { mutableStateOf("") }
    var preselectedArtisanForBooking by remember { mutableStateOf<Artisan?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val feedbackMessage by viewModel.message.collectAsState()

    LaunchedEffect(feedbackMessage) {
        feedbackMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearMessage()
        }
    }

    // Handle back button on sub-screens to return to Home
    if (selectedIndex != 0) {
        BackHandler {
            selectedIndex = 0
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val isWideScreen = maxWidth >= 600.dp

        if (isWideScreen) {
            // Adaptive Tablet / Samsung DeX Layout: Navigation Rail on the side
            Row(modifier = Modifier.fillMaxSize()) {
                NavigationRail(
                    modifier = Modifier.fillMaxHeight(),
                    containerColor = MaterialTheme.colorScheme.surface
                ) {
                    navItems.forEachIndexed { index, item ->
                        NavigationRailItem(
                            selected = selectedIndex == index,
                            onClick = { selectedIndex = index },
                            icon = {
                                Icon(
                                    imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                                    contentDescription = item.title
                                )
                            },
                            label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.testTag(item.testTag)
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    ScreenContent(
                        selectedIndex = selectedIndex,
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        onSignOut = onSignOut,
                        marketplaceCategoryFilter = marketplaceCategoryFilter,
                        marketplaceProblemDesc = marketplaceProblemDesc,
                        preselectedArtisanForBooking = preselectedArtisanForBooking,
                        onNavigateToAi = { selectedIndex = 1 },
                        onNavigateToAssets = { selectedIndex = 2 },
                        onNavigateToMarketplace = { cat ->
                            marketplaceCategoryFilter = cat
                            selectedIndex = 3
                        },
                        onBookArtisan = { artisan ->
                            preselectedArtisanForBooking = artisan
                            selectedIndex = 3
                        },
                        onRequestArtisanForIssue = { category, issueDesc ->
                            marketplaceCategoryFilter = category
                            marketplaceProblemDesc = issueDesc
                            selectedIndex = 3
                        },
                        onClearSelectedArtisan = {
                            preselectedArtisanForBooking = null
                        }
                    )
                }
            }
        } else {
            // Mobile Layout: Bottom Navigation Bar
            Scaffold(
                bottomBar = {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surface
                    ) {
                        navItems.forEachIndexed { index, item ->
                            NavigationBarItem(
                                selected = selectedIndex == index,
                                onClick = { selectedIndex = index },
                                icon = {
                                    Icon(
                                        imageVector = if (selectedIndex == index) item.selectedIcon else item.unselectedIcon,
                                        contentDescription = item.title
                                    )
                                },
                                label = { Text(item.title, style = MaterialTheme.typography.labelSmall) },
                                modifier = Modifier.testTag(item.testTag)
                            )
                        }
                    }
                },
                snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
            ) { paddingValues ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    ScreenContent(
                        selectedIndex = selectedIndex,
                        viewModel = viewModel,
                        isDarkTheme = isDarkTheme,
                        onToggleDarkTheme = onToggleDarkTheme,
                        onSignOut = onSignOut,
                        marketplaceCategoryFilter = marketplaceCategoryFilter,
                        marketplaceProblemDesc = marketplaceProblemDesc,
                        preselectedArtisanForBooking = preselectedArtisanForBooking,
                        onNavigateToAi = { selectedIndex = 1 },
                        onNavigateToAssets = { selectedIndex = 2 },
                        onNavigateToMarketplace = { cat ->
                            marketplaceCategoryFilter = cat
                            selectedIndex = 3
                        },
                        onBookArtisan = { artisan ->
                            preselectedArtisanForBooking = artisan
                            selectedIndex = 3
                        },
                        onRequestArtisanForIssue = { category, issueDesc ->
                            marketplaceCategoryFilter = category
                            marketplaceProblemDesc = issueDesc
                            selectedIndex = 3
                        },
                        onClearSelectedArtisan = {
                            preselectedArtisanForBooking = null
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun ScreenContent(
    selectedIndex: Int,
    viewModel: SiyanetaViewModel,
    isDarkTheme: Boolean,
    onToggleDarkTheme: (Boolean) -> Unit,
    onSignOut: () -> Unit,
    marketplaceCategoryFilter: String?,
    marketplaceProblemDesc: String,
    preselectedArtisanForBooking: Artisan?,
    onNavigateToAi: () -> Unit,
    onNavigateToAssets: () -> Unit,
    onNavigateToMarketplace: (String?) -> Unit,
    onBookArtisan: (Artisan) -> Unit,
    onRequestArtisanForIssue: (String, String) -> Unit,
    onClearSelectedArtisan: () -> Unit
) {
    when (selectedIndex) {
        0 -> HomeScreen(
            viewModel = viewModel,
            onNavigateToAi = onNavigateToAi,
            onNavigateToMarketplace = onNavigateToMarketplace,
            onNavigateToAssets = onNavigateToAssets,
            onBookArtisan = onBookArtisan
        )
        1 -> AiDiagnosticScreen(
            viewModel = viewModel,
            onRequestArtisan = onRequestArtisanForIssue
        )
        2 -> AssetLogbookScreen(
            viewModel = viewModel
        )
        3 -> MarketplaceScreen(
            viewModel = viewModel,
            initialCategoryFilter = marketplaceCategoryFilter,
            initialProblemDescription = marketplaceProblemDesc,
            selectedArtisanForBooking = preselectedArtisanForBooking,
            onClearSelectedArtisan = onClearSelectedArtisan
        )
        4 -> ProfileScreen(
            viewModel = viewModel,
            isDarkTheme = isDarkTheme,
            onToggleDarkTheme = onToggleDarkTheme,
            onSignOut = onSignOut
        )
    }
}
