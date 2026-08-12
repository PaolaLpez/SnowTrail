package mx.utng.snowtrail.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * Tokens de color para la interfaz de Wear OS (Smartwatch).
 * Paleta pastel optimizada para pantallas OLED oscuras.
 */
object SnowTrailColors {
    val Background = Color(0xFF0C0C0E)        // Fondo ultra oscuro para ahorro de batería OLED
    val CardBackground = Color(0xFF1B1B1E)    // Fondo gris oscuro suave para tarjetas
    val PrimaryIce = Color(0xFFFEE1E8)        // Rosa Nieve Pastel
    val PrimaryCream = Color(0xFFE2F9EE)      // Verde Menta Pastel
    val TextPrimary = Color(0xFFFCFAF2)       // Blanco Crema
    val TextSecondary = Color(0xFFC4B8B0)     // Cacao Suave
    val Gold = Color(0xFFFFF0C2)              // Amarillo Miel Pastel
    
    // Colores de Badges de Estado (coincidentes con módulo móvil)
    val StatusNuevo = Color(0xFFFFF9C4)       // Amarillo Pastel
    val StatusAceptado = Color(0xFFE8F5E9)    // Verde Pastel
    val StatusPospuesto = Color(0xFFFFE0B2)   // Naranja Pastel
    val StatusRechazado = Color(0xFFFFEBEE)   // Rojo Pastel
    val StatusEntregado = Color(0xFFE3F2FD)   // Celeste Pastel
}
