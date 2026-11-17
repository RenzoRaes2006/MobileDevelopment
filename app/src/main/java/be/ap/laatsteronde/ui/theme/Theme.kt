package be.ap.laatsteronde.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkPrimary,
    onPrimary = DarkBackground,
    primaryContainer = DarkSelectedTint,
    onPrimaryContainer = DarkText,
    secondary = DarkCarrot,
    onSecondary = DarkBackground,
    secondaryContainer = NeutralIconBox,
    onSecondaryContainer = NeutralIconStroke,
    tertiary = DarkCarrot,
    background = DarkBackground,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurface,
    onSurfaceVariant = DarkTextSoft,
    // Material's default container colors are purple-tinted; use palette colors instead.
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurface,
    surfaceContainer = DarkSurface,
    surfaceContainerHigh = DarkSurface,
    surfaceContainerHighest = DarkSurface,
    outline = DarkTextSoft,
    error = ErrorRed
)

private val LightColorScheme = lightColorScheme(
    primary = ForestGreen,
    onPrimary = SurfaceWhite,
    primaryContainer = SelectedTint,
    onPrimaryContainer = TextDark,
    secondary = CarrotOrange,
    onSecondary = TextDark,
    secondaryContainer = NeutralIconBox,
    onSecondaryContainer = NeutralIconStroke,
    tertiary = CarrotOrange,
    background = BackgroundCream,
    onBackground = TextDark,
    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = Kraft,
    onSurfaceVariant = TextSoft,
    // Material's default container colors are purple-tinted; use palette colors instead.
    surfaceContainerLowest = SurfaceWhite,
    surfaceContainerLow = SurfaceWhite,
    surfaceContainer = SurfaceWhite,
    surfaceContainerHigh = SurfaceWhite,
    surfaceContainerHighest = Kraft,
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
