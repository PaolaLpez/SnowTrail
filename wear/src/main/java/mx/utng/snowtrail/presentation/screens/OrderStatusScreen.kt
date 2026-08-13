package mx.utng.snowtrail.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import mx.utng.snowtrail.model.NeveriaResumen
import mx.utng.snowtrail.model.PedidoResumen
import mx.utng.snowtrail.presentation.theme.SnowTrailColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ARCHIVO: OrderStatusScreen.kt
 * PROPÓSITO: Pantalla 1 de Wear OS (UI Layer).
 * Muestra el estado del pedido activo, tiempo estimado de entrega y las sucursales favoritas más cercanas.
 */

/**
 * Función Composable para la Pantalla 1 de Wear OS (Estado del Pedido y Favoritos Cercanos).
 * 
 * @param order Pedido activo en curso recibido vía DataClient o null.
 * @param nearbyFavorites Lista de neverías favoritas ordenadas por proximidad.
 * @param isConnected Booleano que indica el estado del enlace con el smartphone.
 * @param onOrderClicked Callback al pulsar sobre la tarjeta del pedido.
 * @param onShopClicked Callback al seleccionar una heladería favorita.
 * @param modifier Modificador visual opcional de Compose.
 */
@Composable
fun OrderStatusScreen(
    order: PedidoResumen?,
    nearbyFavorites: List<NeveriaResumen>,
    isConnected: Boolean,
    onOrderClicked: () -> Unit,
    onShopClicked: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    // [CONTENEDOR CIRCULAR DE WEAR OS]: Ajusta el diseño a la pantalla redonda del smartwatch con fondo oscuro
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SnowTrailColors.Background)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        // [COLUMNA PRINCIPAL RECTILÍNEA DE ESTADOS Y NAVEGACIÓN]:
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // [INDICADOR DE ENLACE DE RED HARDWARE / BLUETOOTH]:
            // Muestra un punto verde (Conectado) o rojo (Desconectado) que evalúa el estado del enlace con el smartphone
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Título de la app en tipografía rosa helado
                Text(
                    text = "SnowTrail",
                    fontSize = 11.sp,
                    color = SnowTrailColors.PrimaryIce,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                // Círculo de estatus de red (Punto verde o rojo)
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isConnected) Color.Green else Color.Red,
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            // [EVALUACIÓN DE PEDIDO ACTIVO EN PANTALLA DE RELOJ]:
            if (order != null) {
                // Tarjeta contenedora interactiva para el ticket de la orden activa en cocina
                Card(
                    onClick = onOrderClicked,
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = SnowTrailColors.CardBackground,
                        endBackgroundColor = SnowTrailColors.CardBackground
                    ),
                    modifier = Modifier.fillMaxWidth().height(105.dp),
                    contentPadding = PaddingValues(6.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = order.neveriaNombre,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnowTrailColors.TextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            
                            // Badge de Estado Codificado por Color
                            val badgeColor = when (order.estado) {
                                "NUEVO" -> SnowTrailColors.StatusNuevo
                                "ACEPTADO" -> SnowTrailColors.StatusAceptado
                                "POSPUESTO" -> SnowTrailColors.StatusPospuesto
                                "RECHAZADO" -> SnowTrailColors.StatusRechazado
                                "ENTREGADO" -> SnowTrailColors.StatusEntregado
                                else -> SnowTrailColors.PrimaryIce
                            }
                            
                            Box(
                                modifier = Modifier
                                    .background(badgeColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = order.estado,
                                    color = badgeColor,
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        // Hora estimada de entrega
                        val deliveryTimeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault())
                            .format(Date(order.fechaHoraMillis + order.tiempoEstimadoMinutos * 60000))
                        
                        Text(
                            text = "Entrega aprox: $deliveryTimeFormatted",
                            fontSize = 9.sp,
                            color = SnowTrailColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Resumen de Productos
                        val displayProducts = order.productos.take(2)
                        val extraCount = order.productos.size - 2
                        
                        Column {
                            displayProducts.forEach { prod ->
                                Text(
                                    text = "• ${prod.cantidad}x ${prod.nombre}",
                                    fontSize = 9.sp,
                                    color = SnowTrailColors.TextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (extraCount > 0) {
                                Text(
                                    text = "+$extraCount producto(s) más",
                                    fontSize = 8.sp,
                                    color = SnowTrailColors.PrimaryIce,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Total: $${String.format(Locale.US, "%.2f", order.total)}",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = SnowTrailColors.PrimaryCream
                            )
                        }
                    }
                }
            } else {
                // Bloque sin pedido activo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(SnowTrailColors.CardBackground, RoundedCornerShape(12.dp))
                        .clickable { onOrderClicked() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Sin pedidos activos\n(Toca para ordenar)",
                        fontSize = 11.sp,
                        color = SnowTrailColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 14.sp
                    )
                }
            }

            // Bloque Inferior: 2 favoritos más cercanos
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "FAVORITOS CERCANOS",
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    color = SnowTrailColors.TextSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                if (nearbyFavorites.isEmpty()) {
                    Text(
                        text = "Sin favoritos cerca",
                        fontSize = 9.sp,
                        color = SnowTrailColors.TextSecondary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        nearbyFavorites.take(2).forEach { fav ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(SnowTrailColors.CardBackground, RoundedCornerShape(8.dp))
                                    .clickable { onShopClicked(fav.id) }
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text(
                                        text = fav.nombre,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SnowTrailColors.TextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${fav.distancia.toInt()}m",
                                        fontSize = 8.sp,
                                        color = SnowTrailColors.PrimaryIce
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
