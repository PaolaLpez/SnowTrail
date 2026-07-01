package mx.utng.snowtrail.service

import android.util.Log
import com.google.android.gms.wearable.*
import mx.utng.snowtrail.shared.WearPaths
import mx.utng.snowtrail.model.NeveriaResumen
import mx.utng.snowtrail.model.NotificacionResumen
import mx.utng.snowtrail.model.PedidoResumen
import mx.utng.snowtrail.model.ProductoResumen
import mx.utng.snowtrail.model.ProximityAlert
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.charset.StandardCharsets

/**
 * Global state container for Wear OS UI components, updated reactive-style
 * from incoming DataLayer changes and messages.
 */
object WearStateHolder {
    private val _activeOrder = MutableStateFlow<PedidoResumen?>(
        PedidoResumen(
            id = "order_123",
            neveriaId = "nev_los_abuelos",
            neveriaNombre = "Los Abuelos",
            estado = "NUEVO",
            tiempoEstimadoMinutos = 25,
            fechaHoraMillis = System.currentTimeMillis(),
            total = 180.0,
            productos = listOf(
                ProductoResumen("Nieve de Limón", 2, 45.0),
                ProductoResumen("Helado de Chocolate", 1, 90.0)
            )
        )
    )
    val activeOrder: StateFlow<PedidoResumen?> = _activeOrder

    private val _nearbyShops = MutableStateFlow<List<NeveriaResumen>>(
        listOf(
            NeveriaResumen("nev_los_abuelos", "Los Abuelos", 80.0, true, true),
            NeveriaResumen("nev_la_mich", "La Michoacana", 350.0, true, false),
            NeveriaResumen("nev_zero", "Helados Bajo Cero", 1200.0, false, true),
            NeveriaResumen("nev_artis", "Artesanales del Valle", 2900.0, false, false)
        )
    )
    val nearbyShops: StateFlow<List<NeveriaResumen>> = _nearbyShops

    private val _notifications = MutableStateFlow<List<NotificacionResumen>>(
        listOf(
            NotificacionResumen("notif_1", "Tu pedido ha sido creado", "CAMBIO_ESTADO", false, System.currentTimeMillis() - 600000),
            NotificacionResumen("notif_2", "¡Promoción 2x1 en nieve de fresa!", "PROMOCION", false, System.currentTimeMillis() - 1200000),
            NotificacionResumen("notif_3", "Estás cerca de Helados Bajo Cero", "PROXIMIDAD", true, System.currentTimeMillis() - 3600000)
        )
    )
    val notifications: StateFlow<List<NotificacionResumen>> = _notifications

    private val _proximityAlert = MutableStateFlow<ProximityAlert?>(null)
    val proximityAlert: StateFlow<ProximityAlert?> = _proximityAlert
    
    private val _isLoading = MutableStateFlow(false) // Default to false for standalone demo
    val isLoading: StateFlow<Boolean> = _isLoading

    fun updateActiveOrder(order: PedidoResumen?) {
        _activeOrder.value = order
        _isLoading.value = false
    }

    fun updateNearbyShops(shops: List<NeveriaResumen>) {
        _nearbyShops.value = shops
        _isLoading.value = false
    }

    fun updateNotifications(notifs: List<NotificacionResumen>) {
        _notifications.value = notifs
        _isLoading.value = false
    }

    fun triggerProximityAlert(alert: ProximityAlert?) {
        _proximityAlert.value = alert
    }
    
    fun clearProximityAlert() {
        _proximityAlert.value = null
    }

    fun setLoading(loading: Boolean) {
        _isLoading.value = loading
    }
}

/**
 * Service running on the watch that listens to data sync updates
 * and high-priority proximity message events.
 */
class WearDataListenerService : WearableListenerService() {

    private val tag = "WearDataListener"

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d(tag, "onDataChanged disparado en el reloj.")
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val dataItem = event.dataItem
                val uriPath = dataItem.uri.path ?: continue
                val dataMap = DataMapItem.fromDataItem(dataItem).dataMap

                when (uriPath) {
                    WearPaths.PATH_PEDIDO_ACTIVO -> {
                        val hasActiveOrder = dataMap.getBoolean("hasActiveOrder", false)
                        if (!hasActiveOrder) {
                            WearStateHolder.updateActiveOrder(null)
                        } else {
                            val id = dataMap.getString("id", "")
                            val neveriaId = dataMap.getString("neveriaId", "")
                            val neveriaNombre = dataMap.getString("neveriaNombre", "")
                            val estado = dataMap.getString("estado", "NUEVO")
                            val tiempoEstimado = dataMap.getLong("tiempoEstimadoMinutos", 0)
                            val fechaHora = dataMap.getLong("fechaHoraMillis", 0)
                            val total = dataMap.getDouble("total", 0.0)

                            val productsList = dataMap.getDataMapArrayList("productos") ?: ArrayList()
                            val products = productsList.map { pMap ->
                                ProductoResumen(
                                    nombre = pMap.getString("nombre", ""),
                                    cantidad = pMap.getInt("cantidad", 0),
                                    precioUnitario = pMap.getDouble("precioUnitario", 0.0)
                                )
                            }

                            val order = PedidoResumen(id, neveriaId, neveriaNombre, estado, tiempoEstimado, fechaHora, total, products)
                            WearStateHolder.updateActiveOrder(order)
                            Log.d(tag, "Pedido activo sincronizado: ${order.estado}")
                        }
                    }

                    WearPaths.PATH_NEVERIAS_CERCANAS -> {
                        val shopMaps = dataMap.getDataMapArrayList("shops") ?: ArrayList()
                        val shops = shopMaps.map { sMap ->
                            NeveriaResumen(
                                id = sMap.getString("id", ""),
                                nombre = sMap.getString("nombre", ""),
                                distancia = sMap.getDouble("distancia", 0.0),
                                esFavorita = sMap.getBoolean("esFavorita", false),
                                tienePromocion = sMap.getBoolean("tienePromocion", false)
                            )
                        }
                        WearStateHolder.updateNearbyShops(shops)
                        Log.d(tag, "Neverías cercanas sincronizadas: ${shops.size} locales.")
                    }

                    WearPaths.PATH_NOTIFICACIONES -> {
                        val notifMaps = dataMap.getDataMapArrayList("notifications") ?: ArrayList()
                        val notifs = notifMaps.map { nMap ->
                            NotificacionResumen(
                                id = nMap.getString("id", ""),
                                mensaje = nMap.getString("mensaje", ""),
                                tipo = nMap.getString("tipo", "CAMBIO_ESTADO"),
                                leida = nMap.getBoolean("leida", false),
                                fechaEnvio = nMap.getLong("fechaEnvio", 0)
                            )
                        }
                        WearStateHolder.updateNotifications(notifs)
                        Log.d(tag, "Bandeja de notificaciones sincronizada: ${notifs.size} items.")
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        Log.d(tag, "Mensaje recibido en el reloj. Path: $path")
        
        if (path == WearPaths.PATH_ALARMA_PROXIMIDAD) {
            val payload = String(messageEvent.data, StandardCharsets.UTF_8)
            val parts = payload.split("|")
            if (parts.size >= 2) {
                val shopName = parts[0]
                val distance = parts[1].toIntOrNull() ?: 0
                val note = if (parts.size > 2) parts[2] else ""
                
                // Fire proximity alert callback to show modal
                WearStateHolder.triggerProximityAlert(
                    ProximityAlert(shopName, distance, note)
                )
                Log.d(tag, "Alerta de proximidad disparada: $shopName a $distance m")
            }
        }
    }
}
