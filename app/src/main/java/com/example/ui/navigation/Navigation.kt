package com.example.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.R

sealed class Screen(
    val route: String,
    val titleRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    object Home : Screen("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    object Universities : Screen("universities", R.string.nav_universities, Icons.Filled.School, Icons.Outlined.School)
    object Search : Screen("search", R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search)
    object Downloads : Screen("downloads", R.string.nav_downloads, Icons.Filled.Download, Icons.Outlined.Download)
    object Settings : Screen("settings", R.string.nav_settings, Icons.Filled.Settings, Icons.Outlined.Settings)

    companion object {
        val bottomNavItems = listOf(Home, Universities, Search, Downloads, Settings)

        const val ROUTE_PAPER_DETAIL = "paper_detail/{paperId}"
        const val ROUTE_PDF_VIEWER = "pdf_viewer/{paperId}"
        const val ROUTE_UNIVERSITY_DETAIL = "university_detail/{universityId}"
        const val ROUTE_EXAMS = "exams"

        fun createPaperDetailRoute(paperId: String) = "paper_detail/$paperId"
        fun createPdfViewerRoute(paperId: String) = "pdf_viewer/$paperId"
        fun createUniversityDetailRoute(universityId: String) = "university_detail/$universityId"
    }
}

@Composable
fun AppBottomBar(
    navController: NavController,
    modifier: Modifier = Modifier
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    // Only show on root screens
    val isRootScreen = Screen.bottomNavItems.any { it.route == currentRoute }
    if (!isRootScreen) return

    NavigationBar(
        modifier = modifier.testTag("bottom_nav_bar"),
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface
    ) {
        Screen.bottomNavItems.forEach { screen ->
            val isSelected = currentRoute == screen.route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (currentRoute != screen.route) {
                        navController.navigate(screen.route) {
                            popUpTo(Screen.Home.route) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                        contentDescription = stringResource(screen.titleRes)
                    )
                },
                label = { Text(text = stringResource(screen.titleRes)) },
                modifier = Modifier.testTag("nav_item_${screen.route}")
            )
        }
    }
}
