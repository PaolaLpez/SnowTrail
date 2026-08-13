package mx.utng.snowtrail.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.*
import mx.utng.snowtrail.model.NeveriaResumen
import mx.utng.snowtrail.presentation.theme.SnowTrailColors
import java.util.Locale

/**
 * ARCHIVO: NearbyShopsScreen.kt
 * PROPÓSITO: Pantalla 2 de Wear OS (UI Layer).
 * Lista de Neverías Cercanas con escalado curvo dinámico (ScalingLazyColumn) e indicadores de promoción y favoritos.
 */

/**
 * Función Composable para la Pantalla 2 de Wear OS (Directorio Curvo de Neverías Cercanas).
 * 
 * @param shops Lista de neverías sincronizadas.
 * @param isLoading Estado de carga inicial.
 * @param focusedIndex Índice del elemento con foco al usar el bisel giratorio (Rotary Input).
 * @param onShopSelected Callback disparado al presionar una heladería.
 * @param modifier Modificador visual opcional.
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

                            // Indicador de Estrella de Favorito
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
