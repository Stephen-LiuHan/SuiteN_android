package space.suiten.presentation.navigation

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import space.suiten.SuiteNApplication
import space.suiten.core.service.AIService
import space.suiten.core.service.ImageService
import space.suiten.core.service.OCRService
import space.suiten.data.repository.AIRepositoryImpl
import space.suiten.data.repository.AuthRepositoryImpl
import space.suiten.data.repository.BookRepositoryImpl
import space.suiten.data.repository.CaptureRepositoryImpl
import space.suiten.data.repository.SyncRepositoryImpl
import space.suiten.presentation.screens.*
import space.suiten.presentation.viewmodel.AIViewModel
import space.suiten.presentation.viewmodel.BookViewModel
import space.suiten.presentation.viewmodel.CaptureViewModel
import space.suiten.presentation.viewmodel.SyncViewModel

data class BottomNavItem(
    val screen: Screen,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuiteNNavHost(
    navController: NavHostController = rememberNavController()
) {
    val context = LocalContext.current
    val application = context.applicationContext as SuiteNApplication
    val database = application.database

    val bookRepository = BookRepositoryImpl(database.bookDao())
    val captureRepository = CaptureRepositoryImpl(database.captureDao(), database.readingSessionDao())
    val aiRepository = AIRepositoryImpl(
        database.aiResultDao(),
        database.nextActionDao(),
        database.userProfileDao(),
        database.feedbackLogDao()
    )

    val authRepository = AuthRepositoryImpl()
    val syncRepository = SyncRepositoryImpl(database)

    val imageService = ImageService(context)
    val ocrService = OCRService()
    val aiService = AIService()

    val bookViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        BookViewModel(bookRepository)
    }

    val captureViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        CaptureViewModel(captureRepository, aiRepository, imageService, ocrService, aiService)
    }

    val aiViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        AIViewModel(aiRepository, aiService)
    }

    val syncViewModel = androidx.lifecycle.viewmodel.compose.viewModel {
        SyncViewModel(authRepository, syncRepository)
    }

    val bottomNavItems = listOf(
        BottomNavItem(Screen.Home, Icons.Default.Home, "ホーム"),
        BottomNavItem(Screen.Books, Icons.Default.MenuBook, "本棚"),
        BottomNavItem(Screen.Profile, Icons.Default.Person, "プロフィール")
    )

    Scaffold(
        bottomBar = {
            val navBackStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = navBackStackEntry?.destination
            val currentRoute = currentDestination?.route

            // Hide bottom nav only on specific screens (editing/modal screens)
            val hideOnScreens = listOf(
                Screen.CaptureEdit.route,
                Screen.AIChat.route
            )
            val shouldShowBottomNav = currentRoute != null && !hideOnScreens.any {
                currentRoute.startsWith(it.substringBefore("{"))
            }

            if (shouldShowBottomNav) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy?.any {
                            it.route == item.screen.route
                        } == true

                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) },
                            selected = selected,
                            onClick = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                val books by bookViewModel.books.collectAsState()
                val pendingCaptures by captureViewModel.pendingCaptures.collectAsState()

                LaunchedEffect(Unit) {
                    captureViewModel.loadPendingCaptures()
                }

                HomeScreen(
                    recentBooks = books,
                    pendingCaptures = pendingCaptures,
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetail.createRoute(bookId))
                    },
                    onCaptureClick = { captureId ->
                        navController.navigate(Screen.CaptureDetail.createRoute(captureId))
                    }
                )
            }

            composable(Screen.Books.route) {
                val books by bookViewModel.books.collectAsState()
                BooksScreen(
                    books = books,
                    onBookClick = { bookId ->
                        navController.navigate(Screen.BookDetail.createRoute(bookId))
                    },
                    onAddContent = { contentType, title, author, customTypeName, url, memo ->
                        bookViewModel.createBook(contentType, title, author, customTypeName, url, memo)
                    },
                    onSettingsClick = {
                        navController.navigate(Screen.Settings.route)
                    }
                )
            }

            composable(Screen.BookDetail.route) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
                val books by bookViewModel.books.collectAsState()
                val book = books.find { it.id == bookId }
                val captures by captureViewModel.captures.collectAsState()
                var bookSummary by remember { mutableStateOf<String?>(null) }
                var isGeneratingSummary by remember { mutableStateOf(false) }
                var showEditDialog by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(bookId) {
                    captureViewModel.loadCapturesByBook(bookId)
                }

                // Load existing summary from book if available
                LaunchedEffect(book) {
                    bookSummary = book?.summary
                }

                BookDetailScreen(
                    book = book,
                    captures = captures,
                    bookSummary = bookSummary,
                    isGeneratingSummary = isGeneratingSummary,
                    onBack = { navController.popBackStack() },
                    onCaptureClick = { captureId ->
                        navController.navigate(Screen.CaptureDetail.createRoute(captureId))
                    },
                    onAddCapture = {
                        navController.navigate(Screen.CaptureEdit.createRoute(bookId))
                    },
                    onGenerateSummary = {
                        coroutineScope.launch {
                            isGeneratingSummary = true
                            val captureIds = captures.map { it.id }
                            val summary = aiViewModel.generateBookSummary(
                                bookTitle = book?.title ?: "",
                                contentTypeName = book?.getTypeDisplayName() ?: "本",
                                bookMemo = book?.memo,
                                captureIds = captureIds
                            )
                            bookSummary = summary

                            // Save summary to book
                            book?.let { currentBook ->
                                val updatedBook = currentBook.copy(summary = summary)
                                bookViewModel.updateBook(updatedBook)
                            }
                            isGeneratingSummary = false
                        }
                    },
                    onEditBook = { showEditDialog = true }
                )

                // Edit dialog
                if (showEditDialog && book != null) {
                    EditContentDialog(
                        book = book,
                        onDismiss = { showEditDialog = false },
                        onConfirm = { title, author, customTypeName, url, memo ->
                            bookViewModel.updateBookContent(book, title, author, customTypeName, url, memo)
                            showEditDialog = false
                        }
                    )
                }
            }

            composable(Screen.CaptureEdit.route) { backStackEntry ->
                val bookId = backStackEntry.arguments?.getString("bookId") ?: return@composable
                val coroutineScope = rememberCoroutineScope()

                CaptureEditScreen(
                    bookId = bookId,
                    onBack = { navController.popBackStack() },
                    onSubmit = { emotions, memo, imageUri ->
                        coroutineScope.launch {
                            captureViewModel.createCaptureForBook(
                                bookId = bookId,
                                imageUri = imageUri,
                                emotions = emotions,
                                userMemo = memo.ifBlank { null }
                            )
                            // Go back to BookDetail - AI processing happens in background
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(Screen.CaptureDetail.route) { backStackEntry ->
                val captureId = backStackEntry.arguments?.getString("captureId") ?: return@composable
                var capture by remember { mutableStateOf<space.suiten.domain.model.Capture?>(null) }
                var isReanalyzing by remember { mutableStateOf(false) }
                var ocrText by remember { mutableStateOf<String?>(null) }
                var isExtractingText by remember { mutableStateOf(false) }
                val coroutineScope = rememberCoroutineScope()

                LaunchedEffect(captureId) {
                    capture = captureViewModel.getCaptureById(captureId)
                    // Check if OCR text is already cached
                    ocrText = captureViewModel.getCachedOCRText(captureId)
                }

                // Refresh capture when reanalysis completes
                LaunchedEffect(isReanalyzing) {
                    if (!isReanalyzing) {
                        capture = captureViewModel.getCaptureById(captureId)
                    }
                }

                CaptureDetailScreen(
                    capture = capture,
                    isReanalyzing = isReanalyzing,
                    ocrText = ocrText,
                    isExtractingText = isExtractingText,
                    onBack = { navController.popBackStack() },
                    onViewAIResult = { id ->
                        navController.navigate(Screen.AIResult.createRoute(id))
                    },
                    onReanalyze = { id ->
                        coroutineScope.launch {
                            isReanalyzing = true
                            val success = captureViewModel.reprocessWithAI(id)
                            // Refresh capture to update status
                            capture = captureViewModel.getCaptureById(id)
                            isReanalyzing = false
                            if (success) {
                                // Navigate to AI result after successful reanalysis
                                navController.navigate(Screen.AIResult.createRoute(id))
                            }
                        }
                    },
                    onExtractText = { imagePath ->
                        coroutineScope.launch {
                            isExtractingText = true
                            // Extract and cache OCR text for AI processing
                            ocrText = captureViewModel.extractAndCacheOCRText(captureId, imagePath)
                            isExtractingText = false
                        }
                    },
                    onDelete = { id ->
                        coroutineScope.launch {
                            captureViewModel.clearCachedOCRText(id)
                            captureViewModel.deleteCapture(id)
                            navController.popBackStack()
                        }
                    }
                )
            }

            composable(Screen.AIResult.route) { backStackEntry ->
                val captureId = backStackEntry.arguments?.getString("captureId") ?: return@composable
                val aiResult by aiViewModel.currentAIResult.collectAsState()
                val nextActions by aiViewModel.nextActions.collectAsState()
                val isLoading by aiViewModel.isLoading.collectAsState()
                val captureIsLoading by captureViewModel.isLoading.collectAsState()

                // Combined loading state
                val showLoading = isLoading || captureIsLoading || aiResult == null

                // Start AI processing if needed when screen opens
                LaunchedEffect(captureId) {
                    android.util.Log.d("AIResultScreen", "LaunchedEffect: Starting for captureId=$captureId")
                    // First check if result already exists
                    aiViewModel.loadAIResultByCaptureId(captureId)
                    // Then start AI processing if needed (will check status internally)
                    val processed = captureViewModel.processWithAIIfNeeded(captureId)
                    android.util.Log.d("AIResultScreen", "LaunchedEffect: processWithAIIfNeeded returned $processed")
                }

                // Poll for result until available (AI processing might take time)
                LaunchedEffect(captureId) {
                    // Poll up to 15 times (30 seconds total)
                    var pollCount = 0
                    while (pollCount < 15) {
                        kotlinx.coroutines.delay(2000)
                        pollCount++
                        android.util.Log.d("AIResultScreen", "Polling: Attempt $pollCount, loading result...")
                        aiViewModel.loadAIResultByCaptureId(captureId)

                        // Check if we have a result now
                        if (aiViewModel.currentAIResult.value != null) {
                            android.util.Log.d("AIResultScreen", "Polling: Result found!")
                            break
                        }
                    }
                }

                AIResultScreen(
                    captureId = captureId,
                    aiResult = aiResult,
                    nextActions = nextActions,
                    isLoading = showLoading,
                    onBack = { navController.popBackStack() },
                    onChatClick = { resultId ->
                        navController.navigate(Screen.AIChat.createRoute(resultId))
                    },
                    onAxisSelect = { axis ->
                        aiViewModel.selectAxis(axis)
                    },
                    onNextActionClick = { action ->
                        // Record action click for personalization
                        aiViewModel.recordActionClick(action.type)

                        // Handle action based on type
                        when (action.type) {
                            "books" -> {
                                // Open search with query
                                action.query?.let { query ->
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://www.google.com/search?q=$query+本")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                            "videos" -> {
                                // Open YouTube search with query
                                action.query?.let { query ->
                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        data = Uri.parse("https://www.youtube.com/results?search_query=$query")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                        }
                    },
                    onFeedback = { rating ->
                        aiViewModel.saveFeedback(captureId, rating)
                    }
                )
            }

            composable(Screen.AIChat.route) { backStackEntry ->
                val resultId = backStackEntry.arguments?.getString("resultId") ?: return@composable
                val aiResult by aiViewModel.currentAIResult.collectAsState()
                val chatMessages by aiViewModel.chatMessages.collectAsState()
                val isChatLoading by aiViewModel.isChatLoading.collectAsState()

                LaunchedEffect(resultId) {
                    aiViewModel.initializeChat(resultId)
                }

                AIChatScreen(
                    resultId = resultId,
                    aiResult = aiResult,
                    chatMessages = chatMessages,
                    isLoading = isChatLoading,
                    onBack = { navController.popBackStack() },
                    onSendMessage = { message ->
                        aiViewModel.sendMessage(message)
                    },
                    onQuickAction = { prompt ->
                        aiViewModel.sendQuickAction(prompt)
                    }
                )
            }

            composable(Screen.Profile.route) {
                val userProfile by aiViewModel.userProfile.collectAsState()
                val isPersonalizationEnabled by aiViewModel.isPersonalizationEnabled.collectAsState()

                ProfileScreen(
                    userProfile = userProfile,
                    isPersonalizationEnabled = isPersonalizationEnabled,
                    onTogglePersonalization = { enabled ->
                        aiViewModel.togglePersonalization(enabled)
                    }
                )
            }

            composable(Screen.Settings.route) {
                val books by bookViewModel.books.collectAsState()
                val syncUiState by syncViewModel.uiState.collectAsState()

                SettingsScreen(
                    books = books,
                    syncUiState = syncUiState,
                    onBack = { navController.popBackStack() },
                    onDeleteAllData = {
                        // Delete all books (cascade will delete related data)
                        books.forEach { book ->
                            bookViewModel.deleteBook(book.id)
                        }
                    },
                    onDeleteBookData = { bookId ->
                        bookViewModel.deleteBook(bookId)
                    },
                    onLogin = { email, password ->
                        syncViewModel.login(email, password)
                    },
                    onRegister = { email, password ->
                        syncViewModel.register(email, password)
                    },
                    onLogout = {
                        syncViewModel.logout()
                    },
                    onSync = {
                        syncViewModel.sync()
                    },
                    onClearError = {
                        syncViewModel.clearError()
                    }
                )
            }
        }
    }
}
