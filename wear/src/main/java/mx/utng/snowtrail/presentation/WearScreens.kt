package mx.utng.snowtrail.presentation

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import mx.utng.snowtrail.model.NeveriaResumen
import mx.utng.snowtrail.model.NotificacionResumen
import mx.utng.snowtrail.model.PedidoResumen
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom Theme Colors matching SnowTrail Premium Aesthetic (Pastel Ice Cream Palette)
object SnowTrailColors {
    val Background = Color(0xFF0C0C0E)        // Deep dark background for OLED saving
    val CardBackground = Color(0xFF1B1B1E)    // Soft dark grey card background
    val PrimaryIce = Color(0xFFFEE1E8)        // Strawberry Pink Pastel
    val PrimaryCream = Color(0xFFE2F9EE)      // Mint Green Pastel
    val TextPrimary = Color(0xFFFCFAF2)       // Creamy Off-White
    val TextSecondary = Color(0xFFC4B8B0)     // Muted Warm Cacao
    val Gold = Color(0xFFFFF0C2)              // Honey Yellow Pastel
    
    // Status Badge Colors (Pastel-themed matching mobile)
    val StatusNuevo = Color(0xFFFFF9C4)       // Pastel Yellow
    val StatusAceptado = Color(0xFFE8F5E9)    // Pastel Green
    val StatusPospuesto = Color(0xFFFFE0B2)   // Pastel Orange
    val StatusRechazado = Color(0xFFFFEBEE)   // Pastel Red
    val StatusEntregado = Color(0xFFE3F2FD)   // Pastel Celeste
}

/**
 * SCREEN 1: Active Order Status or Summary Screen
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SnowTrailColors.Background)
            .padding(8.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header / App Name & Connectivity status
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "SnowTrail",
                    fontSize = 11.sp,
                    color = SnowTrailColors.PrimaryIce,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .background(
                            if (isConnected) Color.Green else Color.Red,
                            shape = RoundedCornerShape(50)
                        )
                )
            }

            if (order != null) {
                // Active Order Block
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
                            
                            // Color-coded State Badge
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

                        // Delivery ETA Time
                        val deliveryTimeFormatted = SimpleDateFormat("h:mm a", Locale.getDefault())
                            .format(Date(order.fechaHoraMillis + order.tiempoEstimadoMinutos * 60000))
                        
                        Text(
                            text = "Entrega aprox: $deliveryTimeFormatted",
                            fontSize = 9.sp,
                            color = SnowTrailColors.TextSecondary
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Products summary
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
                // No Active Order Block
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

            // Bottom Block: 2 closest favorites
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

/**
 * SCREEN 2: Nearby Ice Cream Shops List
 */
@Composable
fun NearbyShopsScreen(
    shops: List<NeveriaResumen>,
    isLoading: Boolean,
    focusedIndex: Int,
    onShopSelected: (NeveriaResumen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SnowTrailColors.Background),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(
                    indicatorColor = SnowTrailColors.PrimaryIce,
                    trackColor = SnowTrailColors.CardBackground
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(text = "Buscando neverías...", fontSize = 10.sp, color = SnowTrailColors.TextSecondary)
            }
        } else if (shops.isEmpty()) {
            Text(
                text = "No hay neverías cerca\nen un radio de 3 km.\nUsa tu móvil para explorar.",
                fontSize = 11.sp,
                color = SnowTrailColors.TextSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(12.dp)
            )
        } else {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp)
            ) {
                item {
                    Text(
                        text = "NEVERÍAS CERCANAS",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnowTrailColors.PrimaryIce,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    )
                }

                itemsIndexed(shops) { index, shop ->
                    val isFocused = index == focusedIndex
                    val cardBorder = if (isFocused) {
                        Modifier.background(
                            Brush.linearGradient(listOf(SnowTrailColors.PrimaryIce, SnowTrailColors.PrimaryCream)),
                            shape = RoundedCornerShape(12.dp)
                        ).padding(1.5.dp)
                    } else Modifier

                    Card(
                        onClick = { onShopSelected(shop) },
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = SnowTrailColors.CardBackground,
                            endBackgroundColor = SnowTrailColors.CardBackground
                        ),
                        modifier = cardBorder
                            .fillMaxWidth()
                            .height(55.dp),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shop.nombre,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SnowTrailColors.TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val distText = if (shop.distancia < 1000.0) {
                                        "${shop.distancia.toInt()} m"
                                    } else {
                                        String.format(Locale.US, "%.1f km", shop.distancia / 1000.0)
                                    }
                                    
                                    Text(
                                        text = distText,
                                        fontSize = 9.sp,
                                        color = SnowTrailColors.TextSecondary
                                    )

                                    if (shop.tienePromocion) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Box(
                                            modifier = Modifier
                                                .background(SnowTrailColors.Gold.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
                                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                        ) {
                                            Text(
                                                text = "Promo",
                                                color = SnowTrailColors.Gold,
                                                fontSize = 7.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Favorite Indicator Star
                            Icon(
                                imageVector = if (shop.esFavorita) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "Favorito",
                                tint = if (shop.esFavorita) SnowTrailColors.Gold else SnowTrailColors.TextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 3: Notification Tray or Alerts Screen
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationTrayScreen(
    notifications: List<NotificacionResumen>,
    focusedIndex: Int,
    onNotificationClicked: (NotificacionResumen) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SnowTrailColors.Background),
        contentAlignment = Alignment.Center
    ) {
        if (notifications.isEmpty()) {
            Text(
                text = "Sin notificaciones",
                fontSize = 11.sp,
                color = SnowTrailColors.TextSecondary,
                textAlign = TextAlign.Center
            )
        } else {
            ScalingLazyColumn(
                state = rememberScalingLazyListState(),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp)
            ) {
                item {
                    Text(
                        text = "NOTIFICACIONES",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = SnowTrailColors.PrimaryIce,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                    )
                }

                itemsIndexed(notifications) { index, notif ->
                    val isFocused = index == focusedIndex
                    val cardBorder = if (isFocused) {
                        Modifier.background(
                            Brush.linearGradient(listOf(SnowTrailColors.PrimaryIce, SnowTrailColors.PrimaryCream)),
                            shape = RoundedCornerShape(12.dp)
                        ).padding(1.5.dp)
                    } else Modifier

                    Card(
                        onClick = { onNotificationClicked(notif) },
                        backgroundPainter = CardDefaults.cardBackgroundPainter(
                            startBackgroundColor = SnowTrailColors.CardBackground,
                            endBackgroundColor = SnowTrailColors.CardBackground
                        ),
                        modifier = cardBorder
                            .fillMaxWidth()
                            .height(60.dp),
                        contentPadding = PaddingValues(6.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // Icon according to type
                            val (icon, tint) = when (notif.tipo) {
                                "CAMBIO_ESTADO" -> Pair(Icons.Default.Email, SnowTrailColors.PrimaryIce)
                                "PROMOCION" -> Pair(Icons.Default.VolumeUp, SnowTrailColors.Gold)
                                "PROXIMIDAD" -> Pair(Icons.Default.Place, SnowTrailColors.PrimaryCream)
                                else -> Pair(Icons.Default.Notifications, SnowTrailColors.TextSecondary)
                            }
                            
                            Icon(
                                imageVector = icon,
                                contentDescription = notif.tipo,
                                tint = tint,
                                modifier = Modifier.size(16.dp)
                            )

                            Spacer(modifier = Modifier.width(6.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = notif.mensaje,
                                    fontSize = 10.sp,
                                    color = SnowTrailColors.TextPrimary,
                                    maxLines = 1,
                                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                )

                                val timeText = SimpleDateFormat("h:mm a", Locale.getDefault())
                                    .format(Date(notif.fechaEnvio))
                                
                                Text(
                                    text = timeText,
                                    fontSize = 8.sp,
                                    color = SnowTrailColors.TextSecondary
                                )
                            }

                            Spacer(modifier = Modifier.width(4.dp))

                            // Read/Unread dot indicator
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(
                                        if (notif.leida) Color.Gray else Color.Green,
                                        shape = RoundedCornerShape(50)
                                    )
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class NotificationTypeUI(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val tint: Color,
    val label: String,
    val emoji: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun NotificationDetailScreen(
    notification: NotificacionResumen,
    onBack: () -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScalingLazyListState()
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(SnowTrailColors.Background),
        contentAlignment = Alignment.Center
    ) {
        ScalingLazyColumn(
            state = scrollState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Icon + Category
            item {
                val ui = when (notification.tipo) {
                    "CAMBIO_ESTADO" -> NotificationTypeUI(Icons.Default.Email, SnowTrailColors.PrimaryIce, "ESTADO", "🍦")
                    "PROMOCION" -> NotificationTypeUI(Icons.Default.VolumeUp, SnowTrailColors.Gold, "PROMOCIÓN", "✨")
                    "PROXIMIDAD" -> NotificationTypeUI(Icons.Default.Place, SnowTrailColors.PrimaryCream, "PROXIMIDAD", "📍")
                    else -> NotificationTypeUI(Icons.Default.Notifications, SnowTrailColors.TextSecondary, "AVISO", "🔔")
                }
                
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(ui.tint.copy(alpha = 0.2f), RoundedCornerShape(50))
                            .padding(6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = ui.icon,
                            contentDescription = ui.label,
                            tint = ui.tint,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${ui.emoji} ${ui.label} ${ui.emoji}",
                        color = ui.tint,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
            }
            
            // Detail Card (Enriched for Promotions/Types)
            item {
                Card(
                    onClick = {},
                    backgroundPainter = CardDefaults.cardBackgroundPainter(
                        startBackgroundColor = SnowTrailColors.CardBackground,
                        endBackgroundColor = SnowTrailColors.CardBackground
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 2.dp),
                    contentPadding = PaddingValues(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Title / Message with Marquee if long
                        Text(
                            text = notification.mensaje,
                            color = SnowTrailColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        // Promotion Specific Extra Details (Super cool coupon details)
                        if (notification.tipo == "PROMOCION") {
                            // Extract a percentage or make up a code
                            val discountCode = when {
                                notification.mensaje.contains("10") -> "CREMA10"
                                notification.mensaje.contains("15") -> "NIEVE15"
                                notification.mensaje.contains("20") -> "SWEET20"
                                notification.mensaje.contains("25") -> "FRESA25"
                                notification.mensaje.contains("30") -> "MINT30"
                                notification.mensaje.contains("50") -> "ICE50"
                                else -> "SNOWTRAIL20"
                            }
                            
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SnowTrailColors.Gold.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "CÓDIGO DE CUPÓN",
                                    fontSize = 7.sp,
                                    color = SnowTrailColors.Gold,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = discountCode,
                                    fontSize = 13.sp,
                                    color = SnowTrailColors.TextPrimary,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 1.5.sp,
                                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE)
                                )
                                Text(
                                    text = "⚡ Vence hoy - ¡Presenta en caja!",
                                    fontSize = 7.sp,
                                    color = SnowTrailColors.TextSecondary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else if (notification.tipo == "PROXIMIDAD") {
                            // Proximity Extra Info
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SnowTrailColors.PrimaryCream.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "📍 ¡Estás muy cerca!",
                                    fontSize = 8.sp,
                                    color = SnowTrailColors.PrimaryCream,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Camina unos pasos y canjea tus favoritos",
                                    fontSize = 7.sp,
                                    color = SnowTrailColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else if (notification.tipo == "CAMBIO_ESTADO") {
                            // Order state extra info
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(SnowTrailColors.PrimaryIce.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .padding(6.dp)
                            ) {
                                Text(
                                    text = "🍦 Estatus de Pedido",
                                    fontSize = 8.sp,
                                    color = SnowTrailColors.PrimaryIce,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Revisa los detalles en la pestaña de Pedidos",
                                    fontSize = 7.sp,
                                    color = SnowTrailColors.TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        
                        val timeText = SimpleDateFormat("h:mm a - d MMM", Locale.getDefault())
                            .format(Date(notification.fechaEnvio))
                        
                        Text(
                            text = timeText,
                            color = SnowTrailColors.TextSecondary,
                            fontSize = 8.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            
            // Actions
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                ) {
                    // Open on Phone Button
                    Button(
                        onClick = onConfirm,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = SnowTrailColors.PrimaryIce,
                            contentColor = SnowTrailColors.Background
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp),
                        shape = RoundedCornerShape(17.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Ver en Celular",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    // Dismiss Button
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = Color.Red.copy(alpha = 0.2f),
                            contentColor = Color.Red
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(30.dp),
                        shape = RoundedCornerShape(15.dp)
                    ) {
                        Text(
                            text = "Descartar",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF8A80)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    // Back Text Button
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onBack() }
                            .padding(vertical = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Regresar",
                            fontSize = 9.sp,
                            color = SnowTrailColors.TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }
    }
}
