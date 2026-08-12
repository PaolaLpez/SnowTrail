package mx.utng.snowtrail.tv.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mx.utng.snowtrail.tv.database.SnowTrailRepository.TvPromotion
import mx.utng.snowtrail.tv.theme.TvThemeColors

/**
 * Pantalla principal de Promociones para Android TV.
 * Muestra un carrusel con auto-desplazamiento cada 4 segundos de las
 * promociones activas en la heladería seleccionada.
 */
@Composable
fun PromotionsTvScreen(
    promotions: List<TvPromotion>,
    selectedShopName: String,
    onNavigateToOrders: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    // Solicitar foco al iniciar para soportar control remoto de TV
    LaunchedEffect(Unit) {
        try {
            delay(100L)
            focusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Auto-scroll automático del carrusel de promociones
    LaunchedEffect(promotions) {
        if (promotions.isNotEmpty()) {
            while (true) {
                delay(4000L)
                currentIndex = (currentIndex + 1) % promotions.size
            }
        }
    }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        TvThemeColors.MintGreen,
                        TvThemeColors.VanillaBackground
                    )
                )
            )
            .padding(24.dp)
            .clickable { onNavigateToOrders() }
    ) {
        // Decoración de fondo
        Text("🍓", fontSize = 48.sp, modifier = Modifier.align(Alignment.TopEnd).padding(end = 120.dp, top = 20.dp).alpha(0.18f))
        Text("🍦", fontSize = 48.sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 40.dp).alpha(0.18f))
        Text("🍨", fontSize = 48.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 40.dp).alpha(0.18f))

        Column(modifier = Modifier.fillMaxSize()) {
            // Header de la TV con nombre de heladería activa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.White, CircleShape)
                            .border(BorderStroke(1.5.dp, TvThemeColors.PinkBorder), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍦", fontSize = 32.sp)
                    }
                    Column {
                        Text(
                            text = "LA NIEVERÍA PASTEL",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = TvThemeColors.CocoaDark
                        )
                        Text(
                            text = "Heladería Activa: $selectedShopName",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TvThemeColors.CocoaMedium
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("🔔 NOTIFICACIONES", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TvThemeColors.CocoaMedium)
                    Text("SUCURSAL MATRIZ", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = TvThemeColors.GoldText)
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "OPCIÓN DE PROMOCIONES",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = TvThemeColors.CocoaDark,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Carrusel de tarjetas de promociones (izquierda - centro - derecha)
            Row(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (promotions.isNotEmpty()) {
                    for (i in -1..1) {
                        val index = (currentIndex + i + promotions.size) % promotions.size
                        val promo = promotions[index]
                        val isCurrent = i == 0

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color.White else Color.White.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(
                                width = if (isCurrent) 3.dp else 1.dp,
                                color = if (isCurrent) TvThemeColors.PinkBorder else Color.LightGray
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isCurrent) 12.dp else 4.dp
                            ),
                            modifier = Modifier
                                .width(if (isCurrent) 340.dp else 260.dp)
                                .height(if (isCurrent) 280.dp else 220.dp)
                                .padding(horizontal = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize().padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCurrent) 90.dp else 65.dp)
                                        .background(TvThemeColors.PinkStrawberry, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = promo.imagen, fontSize = if (isCurrent) 45.sp else 32.sp)
                                }
                                Text(
                                    text = promo.nombre,
                                    fontSize = if (isCurrent) 20.sp else 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = TvThemeColors.CocoaDark,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = promo.nota,
                                    fontSize = if (isCurrent) 12.sp else 10.sp,
                                    color = TvThemeColors.CocoaMedium,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    Text("No hay promociones activas actualmente.", fontSize = 18.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de navegación a pedidos
            Button(
                onClick = {
                    android.widget.Toast.makeText(context, "Cargando Pedidos...", android.widget.Toast.LENGTH_SHORT).show()
                    onNavigateToOrders()
                },
                colors = ButtonDefaults.buttonColors(containerColor = TvThemeColors.PinkBorder),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(220.dp)
                    .height(50.dp)
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                Text("🔄 ACTUALIZAR", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
