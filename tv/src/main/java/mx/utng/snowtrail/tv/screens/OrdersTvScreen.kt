package mx.utng.snowtrail.tv.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mx.utng.snowtrail.tv.database.SnowTrailRepository.TvOrder
import mx.utng.snowtrail.tv.theme.TvThemeColors

/**
 * ARCHIVO: OrdersTvScreen.kt
 * PROPÓSITO: Pantalla de Gestión de Pedidos para Android TV (UI Layer).
 * Vista dividida (Split-Screen) en dos columnas:
 * - Izquierda: Pedido NUEVO activo con botones de acción (Aceptar, Posponer, Rechazar).
 * - Derecha: Cola de pedidos PENDIENTES con opción de marcar como Entregado.
 */
@Composable
fun OrdersTvScreen(
    orders: List<TvOrder>,
    selectedShopName: String,
    onUpdateOrder: (String, String) -> Unit,
    onBack: () -> Unit
) {
    // Separar pedidos por estado para mostrar en columnas independientes
    val newOrders = orders.filter { it.estado == "NUEVO" }
    val pendingOrders = orders.filter { it.estado == "PENDIENTE" }
    val focusRequester = remember { FocusRequester() }

    // Solicitar foco para soporte de control remoto de TV
    LaunchedEffect(Unit) {
        try {
            delay(100L)
            focusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(TvThemeColors.VanillaBackground)
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Header con logo y nombre de heladería activa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.White, CircleShape)
                            .border(BorderStroke(1.5.dp, TvThemeColors.PinkBorder), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍦", fontSize = 24.sp)
                    }
                    Column {
                        Text(
                            text = "LA NIEVERÍA PASTEL",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TvThemeColors.CocoaDark
                        )
                        Text(
                            text = "Heladería Activa: $selectedShopName",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvThemeColors.CocoaMedium
                        )
                    }
                }
                Text(
                    text = "PEDIDOS",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = TvThemeColors.CocoaDark
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Vista dividida: Nuevos (izquierda) | Pendientes (derecha)
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Columna izquierda: Pedido NUEVO activo
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(
                        text = "Pedidos Nuevos",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TvThemeColors.CocoaDark,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(TvThemeColors.MintGreen.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.5.dp, TvThemeColors.MintGreen), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        if (newOrders.isNotEmpty()) {
                            val activeNew = newOrders.first()
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text("Cliente: ${activeNew.cliente}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TvThemeColors.CocoaDark)
                                    Text(activeNew.paraRecoger, fontSize = 16.sp, color = TvThemeColors.CocoaMedium)
                                    Text("Tiempo de entrega aprox: ${activeNew.tiempoEntrega}", fontSize = 16.sp, color = TvThemeColors.CocoaMedium)
                                    Text("Total: ${activeNew.total}", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TvThemeColors.FresaPink)

                                    Divider(color = Color.LightGray)

                                    Text("Items:", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TvThemeColors.CocoaDark)
                                    Text(activeNew.items.replace(", ", "\n"), fontSize = 16.sp, color = TvThemeColors.CocoaDark)
                                }

                                // Botones de acción del pedido nuevo
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Button(
                                        onClick = { onUpdateOrder(activeNew.id, "PENDIENTE") },
                                        colors = ButtonDefaults.buttonColors(containerColor = TvThemeColors.AceptadoGreen),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("✔ Aceptar", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onUpdateOrder(activeNew.id, "PENDIENTE") },
                                        colors = ButtonDefaults.buttonColors(containerColor = TvThemeColors.PospuestoYellow),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("🕒 Posponer", color = TvThemeColors.CocoaBrown, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onUpdateOrder(activeNew.id, "RECHAZADO") },
                                        colors = ButtonDefaults.buttonColors(containerColor = TvThemeColors.RechazadoRed),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("❌ Rechazar", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text("No hay nuevos pedidos.", fontSize = 16.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                // Columna derecha: Cola de pedidos PENDIENTES
                Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                    Text(
                        text = "Pendientes",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = TvThemeColors.CocoaDark,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) {
                        if (pendingOrders.isNotEmpty()) {
                            items(pendingOrders) { order ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.LightGray),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text("# Num. Pedido: ${order.id}", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TvThemeColors.CocoaDark)
                                                Text("Cliente: ${order.cliente}", fontSize = 14.sp, color = Color.Gray)
                                            }
                                            Button(
                                                onClick = { onUpdateOrder(order.id, "ENTREGADO") },
                                                colors = ButtonDefaults.buttonColors(containerColor = TvThemeColors.EntregadoBlueBg),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, TvThemeColors.EntregadoBlueText),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("✔ Entregado", color = TvThemeColors.EntregadoBlueText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text("Items:\n${order.items.replace(", ", "\n")}", fontSize = 13.sp, color = Color.DarkGray)
                                    }
                                }
                            }
                        } else {
                            item {
                                Box(modifier = Modifier.fillMaxWidth().height(150.dp), contentAlignment = Alignment.Center) {
                                    Text("No hay pedidos pendientes.", fontSize = 16.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón para volver a Promociones
            Button(
                onClick = {
                    android.widget.Toast.makeText(context, "Volviendo a Promociones...", android.widget.Toast.LENGTH_SHORT).show()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA7B8C4)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(180.dp)
                    .height(45.dp)
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                Text("🔄 Actualizar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
