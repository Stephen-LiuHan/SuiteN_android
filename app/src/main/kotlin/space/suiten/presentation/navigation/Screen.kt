package space.suiten.presentation.navigation

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Books : Screen("books")
    object BookDetail : Screen("book_detail/{bookId}") {
        fun createRoute(bookId: String) = "book_detail/$bookId"
    }
    object CaptureEdit : Screen("capture_edit/{bookId}") {
        fun createRoute(bookId: String) = "capture_edit/$bookId"
    }
    object CaptureDetail : Screen("capture_detail/{captureId}") {
        fun createRoute(captureId: String) = "capture_detail/$captureId"
    }
    object AIResult : Screen("ai_result/{captureId}") {
        fun createRoute(captureId: String) = "ai_result/$captureId"
    }
    object AIChat : Screen("ai_chat/{resultId}") {
        fun createRoute(resultId: String) = "ai_chat/$resultId"
    }
    object Profile : Screen("profile")
    object Settings : Screen("settings")
}
