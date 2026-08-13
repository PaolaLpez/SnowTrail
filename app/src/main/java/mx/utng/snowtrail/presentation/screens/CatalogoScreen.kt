package mx.utng.snowtrail.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.snowtrail.presentation.theme.MobileThemeColors
import mx.utng.snowtrail.service.MockProductLine
import java.util.Locale

/**
 * ARCHIVO: CatalogoScreen.kt
 * PROPÓSITO: Pantalla de Catálogo de Productos y Carrito de Compras (UI Layer).
 * Presenta el menú de especialidades artesanales con cálculo de precios en tiempo real y flujo de checkout.
 */

/**
 * Función Composable que construye el catálogo interactivo de productos y golosinas.
 * 
 * @param onAddToCart Callback invocado al seleccionar un producto para añadirlo a la orden activa.
 * @param onCheckout Callback ejecutado para procesar el pedido y generar el ticket final.
 */
@Composable
fun CatalogoScreen(
    onAddToCart: (MockProductLine) -> Unit,
    onCheckout: () -> Unit
) {
    // [LISTA DE ESPECIALIDADES ARTESANALES]: Catálogo estático de productos con precios unitarios en MXN
    val catalog = listOf(
        MockProductLine("Copa Helarte Suprema", 1, 95.0),
        MockProductLine("Nieve Artesanal de Limón", 1, 45.0),
        MockProductLine("Cono Doble Fresa y Chocolate", 1, 65.0),
        MockProductLine("Malteada de Vainilla Cacao", 1, 80.0)
    )

    // Contenedor principal
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Título del catálogo
        Text(
            text = "🍨 Menú de Especialidades",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MobileThemeColors.CocoaDarkText
        )
        Text(
            text = "Selecciona tus productos favoritos para ordenar",
            fontSize = 12.sp,
            color = MobileThemeColors.CocoaLightText
        )

        Spacer(modifier = Modifier.height(12.dp))

        // [LISTA OPTIMIZADA REUTILIZABLE]: LazyColumn que renderiza únicamente los productos visibles en pantalla
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(catalog) { prod ->
                // Tarjeta individual para cada helado/nieve del menú
                Card(
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            // Nombre de la especialidad
                            Text(
                                text = prod.nombre,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = MobileThemeColors.CocoaDarkText
                            )
                            // Precio unitario formateado a 2 decimales
                            Text(
                                text = "$${String.format(Locale.US, "%.2f", prod.precioUnitario)} MXN",
                                fontSize = 13.sp,
                                color = MobileThemeColors.PinkText,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        // Botón de acción rápida para agregar al carrito de la orden activa
                        Button(
                            onClick = { onAddToCart(prod) },
                            colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.IceCreamMint),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Agregar", color = MobileThemeColors.MintText, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // [BOTÓN DE CONFIRMACIÓN Y CHECKOUT]: Dispara la creación del ticket y persiste en SQLite
        Button(
            onClick = onCheckout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.PinkText),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.ShoppingBag, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Confirmar y Enviar Pedido", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
