package mx.utng.snowtrail.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.snowtrail.presentation.theme.MobileThemeColors
import mx.utng.snowtrail.service.MockOrder
import java.util.Locale

/**
 * Pantalla de Seguimiento de Pedido Activo (UI Layer).
 * Muestra el desglose del ticket y permite la simulación de estados.
 */
@Composable
fun PedidoActivoScreen(
    order: MockOrder?,
    onSimulateProgress: (String) -> Unit
) {
    if (order == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.Receipt,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MobileThemeColors.CocoaMuted
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("No hay ningún pedido activo en este momento.", color = MobileThemeColors.CocoaMuted)
                Text("Ve al catálogo para realizar tu primera orden.", fontSize = 12.sp, color = MobileThemeColors.CocoaMuted)
            }
        }
    } else {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(3.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ticket: #${order.id}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        val (bg, txt) = when (order.estado) {
                            "NUEVO" -> Pair(MobileThemeColors.NuevoBg, MobileThemeColors.NuevoText)
                            "ACEPTADO" -> Pair(MobileThemeColors.AceptadoBg, MobileThemeColors.AceptadoText)
                            "POSPUESTO" -> Pair(MobileThemeColors.PospuestoBg, MobileThemeColors.PospuestoText)
                            "ENTREGADO" -> Pair(MobileThemeColors.EntregadoBg, MobileThemeColors.EntregadoText)
                            "RECHAZADO" -> Pair(MobileThemeColors.RechazadoBg, MobileThemeColors.RechazadoText)
                            else -> Pair(MobileThemeColors.IceCreamMint, MobileThemeColors.MintText)
                        }
                        Surface(
                            color = bg,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = order.estado,
                                color = txt,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MobileThemeColors.OffWhiteVanilla)
                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Sucursal: ${order.neveriaNombre}", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                    Text("Tiempo estimado: ~${order.tiempoEstimadoMinutos} minutos", fontSize = 12.sp, color = MobileThemeColors.CocoaLightText)

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Productos:", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    order.productos.forEach { p ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("• ${p.cantidad}x ${p.nombre}", fontSize = 13.sp, color = MobileThemeColors.CocoaDarkText)
                            Text("$${String.format(Locale.US, "%.2f", p.precioUnitario * p.cantidad)}", fontSize = 13.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = MobileThemeColors.OffWhiteVanilla)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total Pagado:", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "$${String.format(Locale.US, "%.2f", order.total)} MXN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = MobileThemeColors.PinkText
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text("Simulación de Estados (Sincroniza con el Reloj):", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = { onSimulateProgress("ACEPTADO") },
                    colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.AceptadoBg),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Aceptar", fontSize = 11.sp, color = MobileThemeColors.AceptadoText, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onSimulateProgress("ENTREGADO") },
                    colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.EntregadoBg),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Entregar", fontSize = 11.sp, color = MobileThemeColors.EntregadoText, fontWeight = FontWeight.Bold)
                }
                Button(
                    onClick = { onSimulateProgress("POSPUESTO") },
                    colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.PospuestoBg),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Posponer", fontSize = 11.sp, color = MobileThemeColors.PospuestoText, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
