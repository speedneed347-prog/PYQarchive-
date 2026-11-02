package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.downloads.DownloadsViewModel
import com.example.ui.exam.ExamCountdownScreen
import com.example.ui.exam.ExamCountdownViewModel
import com.example.ui.home.HomeScreen
import com.example.ui.home.HomeViewModel
import com.example.ui.navigation.AppBottomBar
import com.example.ui.navigation.Screen
import com.example.ui.paper.PaperDetailScreen
import com.example.ui.paper.PaperDetailViewModel
import com.example.ui.pdf.PdfViewerScreen
import com.example.ui.pdf.PdfViewerViewModel
import com.example.ui.search.SearchScreen
import com.example.ui.search.SearchViewModel
import com.example.ui.settings.AppThemeMode
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.PYQArchiveTheme
import com.example.ui.university.UniversitiesScreen
import com.example.ui.university.UniversityDetailScreen
import com.example.ui.university.UniversityViewModel

class MainActivity : ComponentActivity() {

    private val homeViewModel: HomeViewModel by viewModels()
    private val universityViewModel: UniversityViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val paperDetailViewModel: PaperDetailViewModel by viewModels()
    private val pdfViewerViewModel: PdfViewerViewModel by viewModels()
    private val downloadsViewModel: DownloadsViewModel by viewModels()
    private val examCountdownViewModel: ExamCountdownViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val settingsUiState by settingsViewModel.uiState.collectAsState()
            val isDark = when (settingsUiState.currentTheme) {
                AppThemeMode.SYSTEM -> isSystemInDarkTheme()
                AppThemeMode.LIGHT -> false
                AppThemeMode.DARK -> true
            }

            PYQArchiveTheme(darkTheme = isDark) {
                val navController = rememberNavController()

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        AppBottomBar(navController = navController)
                    }
                ) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route,
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // 1. Home
                        composable(Screen.Home.route) {
                            HomeScreen(
                                viewModel = homeViewModel,
                                onNavigateToSearch = { navController.navigate(Screen.Search.route) },
                                onNavigateToUniversities = { navController.navigate(Screen.Universities.route) },
                                onNavigateToUniversityDetail = { id ->
                                    navController.navigate(Screen.createUniversityDetailRoute(id))
                                },
                                onNavigateToPaperDetail = { id ->
                                    navController.navigate(Screen.createPaperDetailRoute(id))
                                },
                                onNavigateToExams = { navController.navigate(Screen.ROUTE_EXAMS) }
                            )
                        }

                        // 2. Universities List
                        composable(Screen.Universities.route) {
                            UniversitiesScreen(
                                viewModel = universityViewModel,
                                onSelectUniversity = { id ->
                                    navController.navigate(Screen.createUniversityDetailRoute(id))
                                }
                            )
                        }

                        // 3. University Detail
                        composable(
                            route = Screen.ROUTE_UNIVERSITY_DETAIL,
                            arguments = listOf(navArgument("universityId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val universityId = backStackEntry.arguments?.getString("universityId") ?: ""
                            UniversityDetailScreen(
                                universityId = universityId,
                                viewModel = universityViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPaperDetail = { id ->
                                    navController.navigate(Screen.createPaperDetailRoute(id))
                                }
                            )
                        }

                        // 4. Search
                        composable(Screen.Search.route) {
                            SearchScreen(
                                viewModel = searchViewModel,
                                onNavigateToPaperDetail = { id ->
                                    navController.navigate(Screen.createPaperDetailRoute(id))
                                }
                            )
                        }

                        // 5. Paper Detail
                        composable(
                            route = Screen.ROUTE_PAPER_DETAIL,
                            arguments = listOf(navArgument("paperId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val paperId = backStackEntry.arguments?.getString("paperId") ?: ""
                            PaperDetailScreen(
                                paperId = paperId,
                                viewModel = paperDetailViewModel,
                                onNavigateBack = { navController.popBackStack() },
                                onNavigateToPdfViewer = { id ->
                                    navController.navigate(Screen.createPdfViewerRoute(id))
                                }
                            )
                        }

                        // 6. Native PDF Viewer
                        composable(
                            route = Screen.ROUTE_PDF_VIEWER,
                            arguments = listOf(navArgument("paperId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val paperId = backStackEntry.arguments?.getString("paperId") ?: ""
                            PdfViewerScreen(
                                paperId = paperId,
                                viewModel = pdfViewerViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 7. Downloads & Bookmarks
                        composable(Screen.Downloads.route) {
                            DownloadsScreen(
                                viewModel = downloadsViewModel,
                                onNavigateToPdfViewer = { id ->
                                    navController.navigate(Screen.createPdfViewerRoute(id))
                                },
                                onNavigateToPaperDetail = { id ->
                                    navController.navigate(Screen.createPaperDetailRoute(id))
                                }
                            )
                        }

                        // 8. Exam Schedules & Countdown
                        composable(Screen.ROUTE_EXAMS) {
                            ExamCountdownScreen(
                                viewModel = examCountdownViewModel,
                                onNavigateBack = { navController.popBackStack() }
                            )
                        }

                        // 9. Settings
                        composable(Screen.Settings.route) {
                            SettingsScreen(
                                viewModel = settingsViewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
