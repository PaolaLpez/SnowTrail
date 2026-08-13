package mx.utng.snowtrail.tv.theme

import androidx.compose.ui.graphics.Color

/**
 * ARCHIVO: TvThemeColors.kt
 * PROPÓSITO: Sistema de diseño y tokens de color para Android TV (UI Layer).
 * Paleta pastel optimizada para pantallas grandes de mostrador: Vainilla, Menta, Fresa, Chocolate y estados de pedidos.
 */
object TvThemeColors {
    val VanillaBackground = Color(0xFFFCFAF2)   // Fondo vainilla suave
    val MintGreen = Color(0xFFE2F9EE)            // Verde menta para paneles
    val PinkStrawberry = Color(0xFFFEE1E8)       // Rosa fresa para acentos
    val PinkBorder = Color(0xFFEF9A9A)           // Borde rosa pastel
    val CocoaDark = Color(0xFF3E2723)            // Texto marrón oscuro
    val CocoaMedium = Color(0xFF795548)          // Texto marrón medio
    val GoldText = Color(0xFF8F6300)             // Texto dorado
    val FresaPink = Color(0xFFB52D5E)            // Rosa intenso (totales, acentos)

    // Estados de pedidos
    val AceptadoGreen = Color(0xFF81C784)        // Verde para Aceptar
    val PospuestoYellow = Color(0xFFFFD54F)      // Amarillo para Posponer
    val RechazadoRed = Color(0xFFE57373)         // Rojo para Rechazar
    val EntregadoBlueBg = Color(0xFFE3F2FD)      // Azul claro para Entregado
    val EntregadoBlueText = Color(0xFF1565C0)    // Azul oscuro para Entregado
    val CocoaBrown = Color(0xFF5D4037)           // Marrón para texto sobre amarillo
}
