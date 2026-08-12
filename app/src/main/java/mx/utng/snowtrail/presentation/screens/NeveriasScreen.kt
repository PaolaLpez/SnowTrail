package mx.utng.snowtrail.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Icecream
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.snowtrail.presentation.theme.MobileThemeColors
import mx.utng.snowtrail.service.MockShop

/**
 * Pantalla de Explorador de Neverías (UI Layer).
 * Permite filtrar entre todas las sucursales y las marcadas como favoritas independientes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NeveriasScreen(
    shops: List<MockShop>,
    showFavoritesOnly: Boolean,
    onToggleFilter: () -> Unit,
    onToggleFavorite: (String) -> Unit,
    onShopClick: (MockShop) -> Unit = {}
) {
    val filteredShops = if (showFavoritesOnly) shops.filter { it.esFavorita } else shops

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Encabezado con selector de filtro Todas / Favoritas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showFavoritesOnly) "⭐ Sucursales Favoritas" else "🍦 Todas las Neverías",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MobileThemeColors.CocoaDarkText
            )
            FilterChip(
                selected = showFavoritesOnly,
                onClick = onToggleFilter,
                label = { Text(if (showFavoritesOnly) "Ver Todas" else "⭐ Favoritas") }
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredShops.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No tienes neverías marcadas como favoritas.\n¡Toca la estrella en cualquier sucursal para guardarla!",
                    textAlign = TextAlign.Center,
                    color = MobileThemeColors.CocoaMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filteredShops) { shop ->
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        elevation = CardDefaults.cardElevation(2.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShopClick(shop) }
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(MobileThemeColors.IceCreamPink, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Icecream,
                                    contentDescription = null,
                                    tint = MobileThemeColors.PinkText
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = shop.nombre,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = MobileThemeColors.CocoaDarkText
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Place,
                                        contentDescription = null,
                                        tint = MobileThemeColors.MintText,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "${shop.distancia.toInt()} m de distancia",
                                        fontSize = 12.sp,
                                        color = MobileThemeColors.CocoaLightText
                                    )
                                    if (shop.tienePromocion) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Surface(
                                            color = MobileThemeColors.GoldPastel,
                                            shape = RoundedCornerShape(6.dp)
                                        ) {
                                            Text(
                                                text = "🔥 Promo",
                                                color = MobileThemeColors.GoldText,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            IconButton(onClick = { onToggleFavorite(shop.id) }) {
                                Icon(
                                    imageVector = if (shop.esFavorita) Icons.Default.Star else Icons.Default.StarBorder,
                                    contentDescription = "Favorito",
                                    tint = if (shop.esFavorita) MobileThemeColors.GoldBorder else MobileThemeColors.CocoaMuted
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
