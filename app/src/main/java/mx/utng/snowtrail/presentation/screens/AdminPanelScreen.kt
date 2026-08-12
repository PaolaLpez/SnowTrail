package mx.utng.snowtrail.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.snowtrail.presentation.theme.MobileThemeColors
import mx.utng.snowtrail.service.MockOrder

/**
 * Panel de Administración (UI Layer).
 * Cuadrícula de botones 2x2 para cambiar los estados de los pedidos.
 */
@Composable
fun AdminPanelScreen(
    activeOrder: MockOrder?,
    onUpdateState: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            text = "🛠️ Panel de Gestión (ADMIN)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MobileThemeColors.CocoaDarkText
        )
        Text(
            text = "Control de pedidos en cocina y máquina de estados",
            fontSize = 12.sp,
            color = MobileThemeColors.CocoaLightText
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (activeOrder != null) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Pedido Actual: #${activeOrder.id} - ${activeOrder.neveriaNombre}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "Estado actual: ${activeOrder.estado}",
                        color = MobileThemeColors.PinkText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("Acciones de Transición de Pedido:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onUpdateState("ACEPTADO") },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.AceptadoBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("✅ Aceptar", fontWeight = FontWeight.Bold, color = MobileThemeColors.AceptadoText)
                    }
                    Button(
                        onClick = { onUpdateState("POSPUESTO") },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.PospuestoBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("⏳ Posponer", fontWeight = FontWeight.Bold, color = MobileThemeColors.PospuestoText)
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(
                        onClick = { onUpdateState("ENTREGADO") },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.EntregadoBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("🎉 Entregar", fontWeight = FontWeight.Bold, color = MobileThemeColors.EntregadoText)
                    }
                    Button(
                        onClick = { onUpdateState("RECHAZADO") },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.RechazadoBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("❌ Rechazar", fontWeight = FontWeight.Bold, color = MobileThemeColors.RechazadoText)
                    }
                }
            }
        } else {
            Text("No hay pedidos pendientes para gestionar.", color = MobileThemeColors.CocoaMuted)
        }
    }
}
