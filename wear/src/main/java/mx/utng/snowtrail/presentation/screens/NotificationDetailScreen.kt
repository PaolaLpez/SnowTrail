package mx.utng.snowtrail.presentation.screens

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
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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

private data class NotificationTypeUI(
    val icon: ImageVector,
    val tint: Color,
    val label: String,
    val emoji: String
)

/**
 * Pantalla Modal de Detalle de Notificación (Cupones, Promociones y Avisos).
 */
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
            // Encabezado: Ícono + Categoría
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
            
            // Tarjeta de Detalle (Enriquecida para Promociones/Tipos)
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
                        // Título / Mensaje con marquesina si es largo
                        Text(
                            text = notification.mensaje,
                            color = SnowTrailColors.TextPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center,
                            lineHeight = 14.sp,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        
                        // Detalles específicos de Promoción (Cupones de Descuento)
                        if (notification.tipo == "PROMOCION") {
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
                            // Info extra de proximidad
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
                            // Info extra de estado del pedido
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
            
            // Acciones
            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                ) {
                    // Botón para ver en el teléfono
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
                    
                    // Botón Descartar
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
                    
                    // Botón Regresar
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
