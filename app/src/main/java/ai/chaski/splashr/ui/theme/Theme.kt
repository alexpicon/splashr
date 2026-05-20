package ai.chaski.splashr.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val LightColors = lightColorScheme(
    primary = Indigo40,
    secondary = Violet40,
    tertiary = Coral40,
    background = LightBackground,
    surface = LightSurface,
    surfaceVariant = LightSurfaceVariant,
)

private val DarkColors = darkColorScheme(
    primary = Indigo80,
    secondary = Violet80,
    tertiary = Coral80,
    background = DarkBackground,
    surface = DarkSurface,
    surfaceVariant = DarkSurfaceVariant,
)

/**
 * Splashr's Material 3 theme. Uses Android 12+ dynamic colour when available,
 * and falls back to the brand palette on older devices.
 */
@Composable
fun SplashrTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColors
        else -> LightColors
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = SplashrTypography,
        content = content,
    )
}
