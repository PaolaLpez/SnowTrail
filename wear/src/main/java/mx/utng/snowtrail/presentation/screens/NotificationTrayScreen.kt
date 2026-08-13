package mx.utng.snowtrail.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import mx.utng.snowtrail.model.NotificacionResumen
import mx.utng.snowtrail.presentation.theme.SnowTrailColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * ARCHIVO: NotificationTrayScreen.kt
 * PROPÓSITO: Pantalla 3 de Wear OS (UI Layer).
 * Bandeja de Notificaciones y Avisos con texto deslizante continuo mediante el modificador basicMarquee.
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
                            // Ícono según el tipo
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

                            // Punto indicador de leído / no leído
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
