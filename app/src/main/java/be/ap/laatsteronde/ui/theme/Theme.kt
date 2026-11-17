package be.ap.laatsteronde.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    secondary = DarkCarrot,
    onSecondary = DarkBackground,
    tertiary = DarkCarrot,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSoft,
    outline = DarkTextSoft,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = SurfaceWhite,
    secondary = CarrotOrange,
    onSecondary = TextDark,
    tertiary = CarrotOrange,
    background = BackgroundCream,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = Kraft,
    onSurfaceVariant = TextSoft,
    outline = FieldBorder,
    error = ErrorRed
)

// No dynamic color on purpose: the app keeps its own Laatste Ronde colors.
@Composable
fun OSMTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        content = content
    )
}
