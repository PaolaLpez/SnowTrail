package mx.utng.snowtrail.presentation.theme

import androidx.compose.ui.graphics.Color

/**
 * ARCHIVO: Color.kt
 * PROPÓSITO: Sistema de diseño y tokens de color pastel (MobileThemeColors).
 * Define la paleta visual para la aplicación móvil: Fresa, Menta, Vainilla, Melocotón, Lavanda, Miel y Cacao.
 */
object MobileThemeColors {
    val OffWhiteVanilla = Color(0xFFFCFAF2)    // Fondo cálido tono crema vainilla
    val PureWhiteCard = Color(0xFFFFFFFF)      // Fondo de tarjetas
    
    // Tonos pastel
    val IceCreamPink = Color(0xFFFEE1E8)       // Fresa
    val PinkText = Color(0xFFB52D5E)
    
    val IceCreamMint = Color(0xFFE2F9EE)       // Menta
    val MintText = Color(0xFF1E6F40)
    
    val IceCreamPeach = Color(0xFFFFEAE2)      // Melocotón
    val PeachText = Color(0xFFBF3E15)
    
    val IceCreamLavender = Color(0xFFECEBFF)   // Lavanda
    val LavenderText = Color(0xFF4A34AC)
    
    val GoldPastel = Color(0xFFFFF0C2)         // Miel / Dorado
    val GoldText = Color(0xFF8F6300)
    val GoldBorder = Color(0xFFFFD54F)
    
    // Tipografía Cacao
    val CocoaDarkText = Color(0xFF3E2723)      // Marrón cacao oscuro para texto principal
    val CocoaLightText = Color(0xFF795548)     // Chocolate con leche para texto secundario
    val CocoaMuted = Color(0xFFA1887F)         // Tono cacao atenuado
    
    // Colores de cápsulas de estado de pedido
    val NuevoBg = Color(0xFFFFF9C4)
    val NuevoText = Color(0xFFF57F17)
    
    val AceptadoBg = Color(0xFFE8F5E9)
    val AceptadoText = Color(0xFF2E7D32)
    
    val PospuestoBg = Color(0xFFFFE0B2)
    val PospuestoText = Color(0xFFE65100)
    
    val RechazadoBg = Color(0xFFFFEBEE)
    val RechazadoText = Color(0xFFC62828)
    
    val EntregadoBg = Color(0xFFE3F2FD)
    val EntregadoText = Color(0xFF1565C0)
}
