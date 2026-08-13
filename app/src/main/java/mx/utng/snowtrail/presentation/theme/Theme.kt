package mx.utng.snowtrail.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

/**
 * ARCHIVO: Theme.kt
 * PROPÓSITO: Tema principal de Jetpack Compose para SnowTrail (Material 3).
 * Aplica los tokens pastel de MobileThemeColors al esquema de color claro (lightColorScheme).
 */
@Composable
fun SnowTrailTheme(content: @Composable () -> Unit) {
    val colorScheme = lightColorScheme(
        primary = MobileThemeColors.PinkText,
        secondary = MobileThemeColors.MintText,
        background = MobileThemeColors.OffWhiteVanilla,
        surface = MobileThemeColors.PureWhiteCard
    )

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}