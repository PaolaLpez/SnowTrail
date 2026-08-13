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
 * ARCHIVO: AdminPanelScreen.kt
 * PROPÓSITO: Panel de Administración (UI Layer).
 * Cuadrícula de botones 2x2 para cambiar los estados de los pedidos en la máquina de estados finita.
 */

/**
 * Función Composable que representa el panel de control del administrador / cocinero.
 * 
 * @param activeOrder Pedido activo a gestionar en la cocina.
 * @param onUpdateState Callback ejecutado al presionar Aceptar, Posponer, Entregar o Rechazar.
 */
@Composable
fun AdminPanelScreen(
    activeOrder: MockOrder?,
    onUpdateState: (String) -> Unit
) {
    // Contenedor principal con margen interno de 16dp
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        // Título del módulo de administración
        Text(
            text = "🛠️ Panel de Gestión (ADMIN)",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MobileThemeColors.CocoaDarkText
        )
        // Subtítulo técnico indicando la función de máquina de estados
        Text(
            text = "Control de pedidos en cocina y máquina de estados",
            fontSize = 12.sp,
            color = MobileThemeColors.CocoaLightText
        )

        Spacer(modifier = Modifier.height(16.dp))

        // [EVALUACIÓN DE ORDEN ACTIVA]: Muestra la tarjeta con los datos del pedido actual si existe
        if (activeOrder != null) {
            Card(
                shape = RoundedCornerShape(14.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // ID del pedido y nombre de la nevería de origen
                    Text(
                        text = "Pedido Actual: #${activeOrder.id} - ${activeOrder.neveriaNombre}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    // Muestra en pantalla el estado actual grabado en SQLite
                    Text(
                        text = "Estado actual: ${activeOrder.estado}",
                        color = MobileThemeColors.PinkText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Etiqueta de la sección de botones de comando
            Text("Acciones de Transición de Pedido:", fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            // [CUADRÍCULA 2X2 DE BOTONES DE ACCIÓN PARA TRANSICIÓN DE ESTADO FINITO]:
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Fila 1: Botones de Aceptar (ACEPTADO) y Posponer (POSPUESTO)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    // Botón para aprobar el pedido y marcarlo como ACEPTADO en la cocina
                    Button(
                        onClick = { onUpdateState("ACEPTADO") },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.AceptadoBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("✅ Aceptar", fontWeight = FontWeight.Bold, color = MobileThemeColors.AceptadoText)
                    }
                    // Botón para pausar o posponer el pedido en la cola
                    Button(
                        onClick = { onUpdateState("POSPUESTO") },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.PospuestoBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("⏳ Posponer", fontWeight = FontWeight.Bold, color = MobileThemeColors.PospuestoText)
                    }
                }

                // Fila 2: Botones de Entregar (ENTREGADO) y Rechazar (RECHAZADO)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    // Botón para marcar la orden como finalizada y entregada al cliente
                    Button(
                        onClick = { onUpdateState("ENTREGADO") },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.EntregadoBg),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Text("🎉 Entregar", fontWeight = FontWeight.Bold, color = MobileThemeColors.EntregadoText)
                    }
                    // Botón para declinar o cancelar el pedido por falta de insumos u otro motivo
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
