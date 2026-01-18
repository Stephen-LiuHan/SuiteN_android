package space.suiten.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Primary,
    onPrimary = Color.White,
    primaryContainer = PrimaryLight,
    secondary = Secondary,
    onSecondary = Color.White,
    background = BackgroundLight,
    surface = SurfaceLight,
    error = Error,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun ShiorioTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
