package mx.utng.snowtrail.presentation.dialogs

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.dialog.Dialog
import mx.utng.snowtrail.model.ProximityAlert
import mx.utng.snowtrail.presentation.theme.SnowTrailColors

/**
 * ARCHIVO: ProximityAlertDialog.kt
 * PROPÓSITO: Diálogo Modal de Alerta de Proximidad para Wear OS.
 * Se despliega automáticamente con vibración háptica al estar a menos de 100m de una tienda, ofreciendo acciones rápidas (Ver / Cerrar).
 */
@Composable
fun ProximityAlertDialog(
    alert: ProximityAlert,
    onOpenShops: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        showDialog = true,
        onDismissRequest = onDismiss
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SnowTrailColors.Background)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "¡Alerta Proximidad!",
                    color = SnowTrailColors.Gold,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "Nevería '${alert.shopName}' a ${alert.distanceMeters}m.",
                    color = SnowTrailColors.TextPrimary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    lineHeight = 15.sp
                )

                if (alert.promoNote.isNotEmpty()) {
                    Text(
                        text = alert.promoNote,
                        color = SnowTrailColors.PrimaryIce,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(backgroundColor = Color.Red.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f).height(24.dp)
                    ) {
                        Text("Cerrar", fontSize = 8.sp, color = Color.Red)
                    }

                    Button(
                        onClick = onOpenShops,
                        colors = ButtonDefaults.buttonColors(backgroundColor = SnowTrailColors.PrimaryIce.copy(alpha = 0.2f)),
                        modifier = Modifier.weight(1f).height(24.dp)
                    ) {
                        Text("Ver", fontSize = 8.sp, color = SnowTrailColors.PrimaryIce)
                    }
                }
            }
        }
    }
}
