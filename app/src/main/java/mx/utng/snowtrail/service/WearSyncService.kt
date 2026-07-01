package mx.utng.snowtrail.service

import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.*
import mx.utng.snowtrail.shared.WearPaths
import mx.utng.snowtrail.MainActivity
import mx.utng.snowtrail.database.SnowTrailRepository
import java.nio.charset.StandardCharsets
import java.util.UUID

/**
 * Service running on the mobile phone that synchronizes state with Wear OS
 * and listens to physical button actions/events sent from the watch.
 */
class WearSyncService : WearableListenerService() {

    private val tag = "WearSyncService"
    private val handler = Handler(Looper.getMainLooper())

    private val repository by lazy { SnowTrailRepository(applicationContext) }

    // Mock Database State for Demonstration & Verification
    companion object {
        var activeOrderState: MockOrder? = null

        val mockShops = mutableListOf<MockShop>()

        val mockNotifications = mutableListOf<MockNotification>()
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(tag, "WearSyncService iniciado en el teléfono.")
        repository.initializeDemoDataIfEmpty()
        loadDataFromDatabase()
        syncAllData()
    }

    private fun loadDataFromDatabase() {
        activeOrderState = repository.getActiveOrder()
        mockShops.clear()
        mockShops.addAll(repository.getShops())
        mockNotifications.clear()
        mockNotifications.addAll(repository.getNotifications())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            val action = it.action
            Log.d(tag, "onStartCommand recibido con acción: $action")
            when (action) {
                "mx.utng.snowtrail.ACTION_SYNC_ALL" -> {
                    loadDataFromDatabase()
                    syncAllData()
                }
                "mx.utng.snowtrail.ACTION_SYNC_ORDER" -> {
                    activeOrderState?.let { order -> repository.saveOrder(order) }
                    loadDataFromDatabase()
                    syncActiveOrder()
                }
                "mx.utng.snowtrail.ACTION_SYNC_SHOPS" -> {
                    loadDataFromDatabase()
                    syncNearbyShops()
                }
                "mx.utng.snowtrail.ACTION_SYNC_NOTIFICATIONS" -> {
                    loadDataFromDatabase()
                    syncNotifications()
                }
                "mx.utng.snowtrail.ACTION_TRIGGER_PROXIMITY" -> {
                    val shopId = it.getStringExtra("shop_id") ?: "nev_los_abuelos"
                    simulateProximityAlert(shopId)
                }
            }
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        val path = messageEvent.path
        val payload = String(messageEvent.data, StandardCharsets.UTF_8)
        Log.d(tag, "Mensaje recibido desde el reloj. Path: $path, Payload: $payload")

        when (path) {
            WearPaths.MSG_ACEPTAR_PEDIDO -> {
                handleOrderStateTransition("ACEPTAR_PEDIDO")
            }
            WearPaths.MSG_ENTREGAR_PEDIDO -> {
                handleOrderStateTransition("ENTREGAR_PEDIDO")
            }
            WearPaths.MSG_POSPONER_PEDIDO -> {
                handleOrderStateTransition("POSPONER_PEDIDO")
            }
            WearPaths.MSG_RECHAZAR_PEDIDO -> {
                handleOrderStateTransition("RECHAZAR_PEDIDO")
            }
            WearPaths.MSG_TOGGLE_FAVORITO -> {
                handleToggleFavorito(payload)
            }
            WearPaths.MSG_ABRIR_DETALLE_NEVERIA -> {
                handleOpenShopOnPhone(payload)
            }
            WearPaths.MSG_ABRIR_NOTIFICACION -> {
                handleOpenNotification(payload)
            }
            WearPaths.MSG_DESCARTAR_NOTIFICACION -> {
                handleDismissNotification(payload)
            }
            WearPaths.PATH_HEARTBEAT -> {
                Log.d(tag, "Heartbeat recibido del reloj.")
            }
            else -> {
                Log.w(tag, "Ruta de mensaje no reconocida: $path")
            }
        }
    }

    /**
     * Handles order state transitions matching the domain rules and invariants:
     * - Accept Order: NUEVO -> ACEPTADO
     * - Deliver Order: ACEPTADO -> ENTREGADO
     * - Postpone Order: NUEVO -> POSPUESTO
     * - Reject Order: POSPUESTO -> RECHAZADO
     */
    private fun handleOrderStateTransition(action: String) {
        val currentOrder = repository.getActiveOrder()
        if (currentOrder == null) {
            Log.e(tag, "Error: No hay pedido activo para transicionar.")
            return
        }

        val currentState = currentOrder.estado
        var newState = currentState
        var transitionValid = false

        when (action) {
            "ACEPTAR_PEDIDO" -> {
                if (currentState == "NUEVO") {
                    newState = "ACEPTADO"
                    transitionValid = true
                }
            }
            "ENTREGAR_PEDIDO" -> {
                if (currentState == "ACEPTADO") {
                    newState = "ENTREGADO"
                    transitionValid = true
                }
            }
            "POSPONER_PEDIDO" -> {
                if (currentState == "NUEVO") {
                    newState = "POSPUESTO"
                    transitionValid = true
                }
            }
            "RECHAZAR_PEDIDO" -> {
                if (currentState == "POSPUESTO") {
                    newState = "RECHAZADO"
                    transitionValid = true
                }
            }
        }

        if (transitionValid) {
            currentOrder.estado = newState
            repository.saveOrder(currentOrder)
            Log.d(tag, "Pedido transicionado con éxito: $currentState -> $newState")
            
            val alertMsg = when (newState) {
                "ACEPTADO" -> "Tu pedido en '${currentOrder.neveriaNombre}' ha sido aceptado. Tiempo de entrega: ${currentOrder.tiempoEstimadoMinutos} min."
                "ENTREGADO" -> "¡Pedido entregado! Gracias por comprar en '${currentOrder.neveriaNombre}'."
                "POSPUESTO" -> "Tu pedido en '${currentOrder.neveriaNombre}' ha sido pospuesto temporalmente."
                "RECHAZADO" -> "Tu pedido en '${currentOrder.neveriaNombre}' ha sido rechazado."
                else -> "El estado de tu pedido en '${currentOrder.neveriaNombre}' ha cambiado a $newState."
            }
            
            val newNotif = MockNotification(
                id = UUID.randomUUID().toString(),
                mensaje = alertMsg,
                tipo = "CAMBIO_ESTADO",
                leida = false,
                fechaEnvio = System.currentTimeMillis()
            )
            repository.addNotification(newNotif)

            loadDataFromDatabase()

            // Sync updated states to watch immediately
            syncActiveOrder()
            syncNotifications()

            // Toast to notify the user on the phone
            handler.post {
                Toast.makeText(this, "Pedido actualizado a $newState (Acción de reloj)", Toast.LENGTH_SHORT).show()
            }
        } else {
            Log.e(tag, "Transición no válida de $currentState usando la acción $action")
        }
    }

    /**
     * Toggles the favorite status of a shop.
     * Updates Firestore/Room, then syncs the updated shop list.
     */
    private fun handleToggleFavorito(shopId: String) {
        val isFavorite = repository.toggleFavoriteShop(shopId)
        loadDataFromDatabase()
        
        Log.d(tag, "Toggle favorito para tienda $shopId: Ahora es $isFavorite")

        // Synchronize with Wear OS
        syncNearbyShops()
        
        handler.post {
            val shopName = mockShops.find { it.id == shopId }?.nombre ?: "Nevería"
            val statusText = if (isFavorite) "agregada a favoritos" else "eliminada de favoritos"
            Toast.makeText(this, "'$shopName' $statusText", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Opens the detail screen of a specific ice cream shop in the phone application.
     */
    private fun handleOpenShopOnPhone(shopId: String) {
        Log.d(tag, "Abriendo pantalla de detalle para la nevería: $shopId en el teléfono")
        handler.post {
            Toast.makeText(this, "Abriendo nevería: $shopId en pantalla móvil", Toast.LENGTH_LONG).show()
        }
        
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("navigation_target", "shop_detail")
            putExtra("shop_id", shopId)
        }
        startActivity(intent)
    }

    /**
     * Opens a specific notification, marking it as read and navigating to its target.
     */
    private fun handleOpenNotification(notifId: String) {
        Log.d(tag, "Abriendo y leyendo notificación: $notifId en el teléfono")
        val notifs = repository.getNotifications()
        val notif = notifs.find { it.id == notifId }
        if (notif != null) {
            repository.markNotificationRead(notifId)
            loadDataFromDatabase()
            syncNotifications()

            handler.post {
                Toast.makeText(this, "Notificación abierta en móvil: ${notif.mensaje}", Toast.LENGTH_LONG).show()
            }

            val intent = Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                putExtra("navigation_target", "notification")
                putExtra("notification_id", notifId)
                putExtra("notification_type", notif.tipo)
            }
            startActivity(intent)
        }
    }

    /**
     * Dismisses a notification, marking it as read and keeping it at the bottom.
     */
    private fun handleDismissNotification(notifId: String) {
        Log.d(tag, "Descartando notificación: $notifId")
        repository.markNotificationRead(notifId)
        loadDataFromDatabase()
        syncNotifications()

        handler.post {
            Toast.makeText(this, "Notificación descartada", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Sends the current active order to Wear OS.
     */
    fun syncActiveOrder() {
        val dataClient = Wearable.getDataClient(this)
        val order = activeOrderState

        if (order == null) {
            val putDataMapReq = PutDataMapRequest.create(WearPaths.PATH_PEDIDO_ACTIVO)
            val dataMap = putDataMapReq.dataMap
            dataMap.putBoolean("hasActiveOrder", false)
            
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq)
                .addOnSuccessListener { Log.d(tag, "Sincronizado: Sin pedido activo") }
                .addOnFailureListener { Log.e(tag, "Fallo al sincronizar vacío de pedido", it) }
        } else {
            val putDataMapReq = PutDataMapRequest.create(WearPaths.PATH_PEDIDO_ACTIVO)
            val dataMap = putDataMapReq.dataMap
            dataMap.putBoolean("hasActiveOrder", true)
            dataMap.putString("id", order.id)
            dataMap.putString("neveriaId", order.neveriaId)
            dataMap.putString("neveriaNombre", order.neveriaNombre)
            dataMap.putString("estado", order.estado)
            dataMap.putLong("tiempoEstimadoMinutos", order.tiempoEstimadoMinutos)
            dataMap.putLong("fechaHoraMillis", order.fechaHoraMillis)
            dataMap.putDouble("total", order.total)

            val prodList = ArrayList<DataMap>()
            for (p in order.productos) {
                val pMap = DataMap()
                pMap.putString("nombre", p.nombre)
                pMap.putInt("cantidad", p.cantidad)
                pMap.putDouble("precioUnitario", p.precioUnitario)
                prodList.add(pMap)
            }
            dataMap.putDataMapArrayList("productos", prodList)

            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            dataClient.putDataItem(putDataReq)
                .addOnSuccessListener { Log.d(tag, "Sincronizado pedido activo: ${order.estado}") }
                .addOnFailureListener { Log.e(tag, "Fallo al sincronizar pedido activo", it) }
        }
    }

    /**
     * Filters nearby shops (<3km), orders them by distance, and syncs the top 10 with Wear OS.
     */
    fun syncNearbyShops() {
        val dataClient = Wearable.getDataClient(this)
        
        val filtered = mockShops
            .filter { it.distancia <= 3000.0 }
            .sortedBy { it.distancia }
            .take(10)

        val putDataMapReq = PutDataMapRequest.create(WearPaths.PATH_NEVERIAS_CERCANAS)
        val dataMap = putDataMapReq.dataMap

        val shopList = ArrayList<DataMap>()
        for (shop in filtered) {
            val shopMap = DataMap()
            shopMap.putString("id", shop.id)
            shopMap.putString("nombre", shop.nombre)
            shopMap.putDouble("distancia", shop.distancia)
            shopMap.putBoolean("esFavorita", shop.esFavorita)
            shopMap.putBoolean("tienePromocion", shop.tienePromocion)
            shopList.add(shopMap)
        }
        dataMap.putDataMapArrayList("shops", shopList)

        val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
        dataClient.putDataItem(putDataReq)
            .addOnSuccessListener { Log.d(tag, "Sincronizadas ${filtered.size} neverías cercanas") }
            .addOnFailureListener { Log.e(tag, "Fallo al sincronizar neverías cercanas", it) }
    }

    /**
     * Synchronizes notifications with Wear OS (Max 10, ordered by newest).
     */
    fun syncNotifications() {
        val dataClient = Wearable.getDataClient(this)
        
        val sortedNotifs = mockNotifications
            .sortedByDescending { it.fechaEnvio }
            .take(10)

        val putDataMapReq = PutDataMapRequest.create(WearPaths.PATH_NOTIFICACIONES)
        val dataMap = putDataMapReq.dataMap

        val notifList = ArrayList<DataMap>()
        for (n in sortedNotifs) {
            val nMap = DataMap()
            nMap.putString("id", n.id)
            nMap.putString("mensaje", n.mensaje)
            nMap.putString("tipo", n.tipo)
            nMap.putBoolean("leida", n.leida)
            nMap.putLong("fechaEnvio", n.fechaEnvio)
            notifList.add(nMap)
        }
        dataMap.putDataMapArrayList("notifications", notifList)

        val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
        dataClient.putDataItem(putDataReq)
            .addOnSuccessListener { Log.d(tag, "Sincronizadas ${sortedNotifs.size} notificaciones") }
            .addOnFailureListener { Log.e(tag, "Fallo al sincronizar notificaciones", it) }
    }

    fun syncAllData() {
        syncActiveOrder()
        syncNearbyShops()
        syncNotifications()
    }

    fun simulateProximityAlert(shopId: String) {
        val shop = mockShops.find { it.id == shopId }
        if (shop != null && (shop.esFavorita || shop.tienePromocion)) {
            val messageClient = Wearable.getMessageClient(this)
            val nodeClient = Wearable.getNodeClient(this)

            nodeClient.connectedNodes.addOnSuccessListener { nodes ->
                val payload = "${shop.nombre}|${shop.distancia.toInt()}|Promo 2x1"
                val dataBytes = payload.toByteArray(StandardCharsets.UTF_8)
                
                for (node in nodes) {
                    messageClient.sendMessage(node.id, WearPaths.PATH_ALARMA_PROXIMIDAD, dataBytes)
                        .addOnSuccessListener { Log.d(tag, "Alerta de proximidad enviada") }
                }
            }
        }
    }
}

data class MockOrder(
    val id: String,
    val neveriaId: String,
    val neveriaNombre: String,
    var estado: String,
    val tiempoEstimadoMinutos: Long,
    val fechaHoraMillis: Long,
    val total: Double,
    val productos: List<MockProductLine>
)

data class MockProductLine(
    val nombre: String,
    val cantidad: Int,
    val precioUnitario: Double
)

data class MockShop(
    val id: String,
    val nombre: String,
    var distancia: Double,
    var esFavorita: Boolean,
    val tienePromocion: Boolean
)

data class MockNotification(
    val id: String,
    val mensaje: String,
    val tipo: String, // CAMBIO_ESTADO, PROMOCION, PROXIMIDAD
    var leida: Boolean,
    val fechaEnvio: Long
)
