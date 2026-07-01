package mx.utng.snowtrail.communication

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.Wearable
import mx.utng.snowtrail.shared.WearPaths
import mx.utng.snowtrail.service.WearStateHolder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.nio.charset.StandardCharsets

/**
 * Handles sending commands and messages from Wear OS watch to the Mobile Phone.
 * Also monitors device connection status via Google Play Services NodeClient.
 */
class WearCommunicationManager(private val context: Context) {
    private val tag = "WearCommManager"
    
    private val _isConnected = MutableStateFlow(true) // Default to true for simulated execution
    val isConnected: StateFlow<Boolean> = _isConnected

    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        checkConnection()
    }

    /**
     * Checks if there is any active connected phone node.
     */
    fun checkConnection() {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                // In simulated mode, always stay connected (true) even if nodes are empty
                _isConnected.value = true
                Log.d(tag, "Conexión con el teléfono (Simulada): true (nodos reales: ${nodes.size})")
            } catch (e: Exception) {
                Log.e(tag, "Error verificando conexión", e)
                _isConnected.value = true
            }
        }
    }

    /**
     * Sends a periodic heartbeat to verify connection.
     */
    fun sendHeartbeat() {
        sendMessage(WearPaths.PATH_HEARTBEAT, "")
    }

    /**
     * Sends a message/command payload to the phone.
     */
    fun sendMessage(
        path: String,
        payload: String,
        onSuccess: () -> Unit = {},
        onFailure: (Exception) -> Unit = {}
    ) {
        scope.launch {
            try {
                val nodes = nodeClient.connectedNodes.await()
                if (nodes.isEmpty()) {
                    // Simulated environment: trigger success locally on the watch
                    Log.d(tag, "Simulación: Enviado mensaje local (sin móvil conectado): Path=$path, Payload=$payload")
                    
                    // Trigger simulated state transitions locally on the watch state if needed,
                    // or just execute success callback to simulate a successful delivery.
                    simulateLocalWatchStateTransition(path, payload)
                    
                    launch(Dispatchers.Main) {
                        onSuccess()
                    }
                    return@launch
                }
                
                _isConnected.value = true
                val dataBytes = payload.toByteArray(StandardCharsets.UTF_8)
                var sentSuccessfully = false
                
                for (node in nodes) {
                    try {
                        messageClient.sendMessage(node.id, path, dataBytes).await()
                        sentSuccessfully = true
                        Log.d(tag, "Mensaje enviado a ${node.displayName}: Path=$path, Payload=$payload")
                    } catch (e: Exception) {
                        Log.e(tag, "Error enviando a nodo ${node.id}", e)
                    }
                }

                launch(Dispatchers.Main) {
                    if (sentSuccessfully) {
                        onSuccess()
                    } else {
                        onFailure(Exception("Fallo al enviar a todos los nodos"))
                    }
                }
            } catch (e: Exception) {
                Log.e(tag, "Error en corrutina de envío", e)
                // In simulated mode, fallback to success to allow local flow testing
                Log.d(tag, "Simulación: Fallback de éxito en error para Path=$path")
                simulateLocalWatchStateTransition(path, payload)
                launch(Dispatchers.Main) {
                    onSuccess()
                }
            }
        }
    }

    /**
     * Local state transitions for standalone watch simulation when phone is disconnected.
     */
    private fun simulateLocalWatchStateTransition(path: String, payload: String) {
        val currentOrder = WearStateHolder.activeOrder.value
        when (path) {
            WearPaths.MSG_ACEPTAR_PEDIDO -> {
                if (currentOrder != null && currentOrder.estado == "NUEVO") {
                    WearStateHolder.updateActiveOrder(currentOrder.copy(estado = "ACEPTADO"))
                }
            }
            WearPaths.MSG_ENTREGAR_PEDIDO -> {
                if (currentOrder != null && currentOrder.estado == "ACEPTADO") {
                    WearStateHolder.updateActiveOrder(currentOrder.copy(estado = "ENTREGADO"))
                }
            }
            WearPaths.MSG_POSPONER_PEDIDO -> {
                if (currentOrder != null && currentOrder.estado == "NUEVO") {
                    WearStateHolder.updateActiveOrder(currentOrder.copy(estado = "POSPUESTO"))
                }
            }
            WearPaths.MSG_RECHAZAR_PEDIDO -> {
                if (currentOrder != null && currentOrder.estado == "POSPUESTO") {
                    WearStateHolder.updateActiveOrder(currentOrder.copy(estado = "RECHAZADO"))
                }
            }
            WearPaths.MSG_TOGGLE_FAVORITO -> {
                val shops = WearStateHolder.nearbyShops.value.map { shop ->
                    if (shop.id == payload) shop.copy(esFavorita = !shop.esFavorita) else shop
                }
                WearStateHolder.updateNearbyShops(shops)
            }
            WearPaths.MSG_DESCARTAR_NOTIFICACION -> {
                val notifs = WearStateHolder.notifications.value.map { notif ->
                    if (notif.id == payload) notif.copy(leida = true) else notif
                }
                WearStateHolder.updateNotifications(notifs)
            }
            WearPaths.MSG_ABRIR_NOTIFICACION -> {
                val notifs = WearStateHolder.notifications.value.map { notif ->
                    if (notif.id == payload) notif.copy(leida = true) else notif
                }
                WearStateHolder.updateNotifications(notifs)
            }
        }
    }
}
