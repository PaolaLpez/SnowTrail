package mx.utng.snowtrail.presentation

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import androidx.wear.compose.material.*
import androidx.wear.compose.material.dialog.Dialog
import mx.utng.snowtrail.shared.WearPaths
import mx.utng.snowtrail.communication.WearCommunicationManager
import mx.utng.snowtrail.model.*
import com.google.android.gms.wearable.*
import mx.utng.snowtrail.service.WearStateHolder
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val tag = "WearMainActivity"
    private lateinit var commManager: WearCommunicationManager
    
    // UI state trackers for physical button navigation
    private var pagerCurrentPage = 1
    private var focusedShopIndex = 0
    private var focusedNotifIndex = 0
    
    // Active Dialog/Notification Detail track
    private var activeDetailNotification by mutableStateOf<NotificacionResumen?>(null)
    private var isProximityAlertActive = false

    @OptIn(ExperimentalFoundationApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        commManager = WearCommunicationManager(this)
        
        // Start connection checks & heartbeats
        startHeartbeatLoop()

        setContent {
            val isConnected by commManager.isConnected.collectAsState()
            val activeOrder by WearStateHolder.activeOrder.collectAsState()
            val nearbyShops by WearStateHolder.nearbyShops.collectAsState()
            val notifications by WearStateHolder.notifications.collectAsState()
            val proximityAlert by WearStateHolder.proximityAlert.collectAsState()
            val isLoading by WearStateHolder.isLoading.collectAsState()

            val pagerState = rememberPagerState(initialPage = 1) { 3 }
            val coroutineScope = rememberCoroutineScope()

            // Foreground listener to synchronize data layer updates in real-time
            DisposableEffect(Unit) {
                val listener = DataClient.OnDataChangedListener { dataEvents ->
                    for (event in dataEvents) {
                        if (event.type == DataEvent.TYPE_CHANGED) {
                            val dataItem = event.dataItem
                            val uriPath = dataItem.uri.path ?: continue
                            val dataMap = DataMapItem.fromDataItem(dataItem).dataMap
                            
                            when (uriPath) {
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
                                }
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
                                    }
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
                                }
                            }
                        }
                    }
                }
                
                val dataClient = Wearable.getDataClient(this@MainActivity)
                dataClient.addListener(listener)
                onDispose {
                    dataClient.removeListener(listener)
                }
            }
            
            // Sync current page with global reference for button handling
            LaunchedEffect(pagerState.currentPage) {
                pagerCurrentPage = pagerState.currentPage
            }

            // Watch for proximity alerts to trigger haptic feedback and show dialog
            LaunchedEffect(proximityAlert) {
                if (proximityAlert != null) {
                    isProximityAlertActive = true
                    triggerHapticFeedback()
                } else {
                    isProximityAlertActive = false
                }
            }

            // Sync index clamps when list sizes change
            LaunchedEffect(nearbyShops) {
                if (focusedShopIndex >= nearbyShops.size && nearbyShops.isNotEmpty()) {
                    focusedShopIndex = nearbyShops.size - 1
                }
            }
            LaunchedEffect(notifications) {
                if (focusedNotifIndex >= notifications.size && notifications.isNotEmpty()) {
                    focusedNotifIndex = notifications.size - 1
                }
            }

            Scaffold(
                modifier = Modifier.fillMaxSize(),
                timeText = { TimeText() }
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    val activeNotif = activeDetailNotification
                    if (activeNotif != null) {
                        NotificationDetailScreen(
                            notification = activeNotif,
                            onBack = { activeDetailNotification = null },
                            onConfirm = {
                                commManager.sendMessage(
                                    WearPaths.MSG_ABRIR_NOTIFICACION, activeNotif.id,
                                    onSuccess = {
                                        activeDetailNotification = null
                                        Toast.makeText(this@MainActivity, "Abriendo en celular...", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onDismiss = {
                                commManager.sendMessage(
                                    WearPaths.MSG_DESCARTAR_NOTIFICACION, activeNotif.id,
                                    onSuccess = {
                                        activeDetailNotification = null
                                    }
                                )
                            }
                        )
                    } else {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            when (page) {
                                0 -> NotificationTrayScreen(
                                    notifications = notifications,
                                    focusedIndex = focusedNotifIndex,
                                    onNotificationClicked = { notif ->
                                        activeDetailNotification = notif
                                    }
                                )
                                1 -> OrderStatusScreen(
                                    order = activeOrder,
                                    nearbyFavorites = nearbyShops.filter { it.esFavorita },
                                    isConnected = isConnected,
                                    onOrderClicked = {
                                        activeOrder?.let {
                                            commManager.sendMessage(WearPaths.MSG_ABRIR_DETALLE_NEVERIA, it.neveriaId)
                                        }
                                    },
                                    onShopClicked = { shopId ->
                                        commManager.sendMessage(WearPaths.MSG_ABRIR_DETALLE_NEVERIA, shopId)
                                    }
                                )
                                2 -> NearbyShopsScreen(
                                    shops = nearbyShops,
                                    focusedIndex = focusedShopIndex,
                                    isLoading = isLoading,
                                    onShopSelected = { shop ->
                                        commManager.sendMessage(WearPaths.MSG_ABRIR_DETALLE_NEVERIA, shop.id)
                                    }
                                )
                            }
                        }
                    }

                    // Connection Status Muted Banner
                    if (!isConnected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Red.copy(alpha = 0.8f))
                                .padding(vertical = 2.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Sin conexión - Reintentando",
                                color = Color.White,
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Proximity Alert Popup / Modal
                    proximityAlert?.let { alert ->
                        ProximityAlertDialog(
                            alert = alert,
                            onOpenShops = {
                                WearStateHolder.clearProximityAlert()
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(2)
                                    val matchIndex = nearbyShops.indexOfFirst { it.nombre.contains(alert.shopName, ignoreCase = true) }
                                    if (matchIndex != -1) {
                                        focusedShopIndex = matchIndex
                                    }
                                }
                            },
                            onDismiss = {
                                WearStateHolder.clearProximityAlert()
                            }
                        )
                    }
                }
            }
        }
    }

    /**
     * Intercepts Wear OS Physical Button events (Stem Keys).
     * Button 1 (Top Button): Primary action, accept state, scroll up.
     * Button 2 (Bottom Button): Secondary action, postpone/cancel, toggle favorite, scroll down.
     */
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(tag, "Físico onKeyDown: KeyCode = $keyCode")
        
        when (keyCode) {
            KeyEvent.KEYCODE_STEM_1 -> { // TOP BUTTON
                handleTopButtonAction()
                return true
            }
            KeyEvent.KEYCODE_STEM_2 -> { // BOTTOM BUTTON
                handleBottomButtonAction()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

    private fun handleTopButtonAction() {
        // Case 1: Proximity alert is active on screen
        if (isProximityAlertActive) {
            val alert = WearStateHolder.proximityAlert.value
            if (alert != null) {
                // Confirm proximity: Open nearby shops list
                WearStateHolder.clearProximityAlert()
                Toast.makeText(this, "Abriendo tiendas cercanas...", Toast.LENGTH_SHORT).show()
            }
            return
        }

        // Case 2: Notification detail modal is active
        val activeNotif = activeDetailNotification
        if (activeNotif != null) {
            commManager.sendMessage(WearPaths.MSG_ABRIR_NOTIFICACION, activeNotif.id)
            activeDetailNotification = null
            return
        }

        // Case 3: Contextual screen behavior based on current page
        when (pagerCurrentPage) {
            0 -> { // NOTIFICATIONS TRAY: Scroll Up / Focus previous item
                val notifs = WearStateHolder.notifications.value
                if (notifs.isNotEmpty()) {
                    focusedNotifIndex = (focusedNotifIndex - 1 + notifs.size) % notifs.size
                    Toast.makeText(this, "Foco: ${notifs[focusedNotifIndex].mensaje.take(15)}...", Toast.LENGTH_SHORT).show()
                }
            }
            1 -> { // ORDER SUMMARY: Confirm or Advance order state
                val order = WearStateHolder.activeOrder.value
                if (order != null) {
                    when (order.estado) {
                        "NUEVO" -> {
                            commManager.sendMessage(WearPaths.MSG_ACEPTAR_PEDIDO, order.id)
                            Toast.makeText(this, "Aceptando pedido...", Toast.LENGTH_SHORT).show()
                        }
                        "ACEPTADO" -> {
                            commManager.sendMessage(WearPaths.MSG_ENTREGAR_PEDIDO, order.id)
                            Toast.makeText(this, "Confirmando entrega...", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Pedido finalizado o sin acción.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Sin pedidos activos.", Toast.LENGTH_SHORT).show()
                }
            }
            2 -> { // NEARBY SHOPS: Select highlighted shop to open detail on mobile
                val shops = WearStateHolder.nearbyShops.value
                if (shops.isNotEmpty() && focusedShopIndex in shops.indices) {
                    val shop = shops[focusedShopIndex]
                    commManager.sendMessage(WearPaths.MSG_ABRIR_DETALLE_NEVERIA, shop.id)
                    Toast.makeText(this, "Abriendo '${shop.nombre}' en celular", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun handleBottomButtonAction() {
        // Case 1: Proximity alert is active on screen
        if (isProximityAlertActive) {
            // Dismiss proximity pop-up
            WearStateHolder.clearProximityAlert()
            return
        }

        // Case 2: Notification detail modal is active
        val activeNotif = activeDetailNotification
        if (activeNotif != null) {
            commManager.sendMessage(WearPaths.MSG_DESCARTAR_NOTIFICACION, activeNotif.id)
            activeDetailNotification = null
            return
        }

        // Case 3: Contextual screen behavior based on current page
        when (pagerCurrentPage) {
            0 -> { // NOTIFICATIONS TRAY: Scroll Down / Focus next item
                val notifs = WearStateHolder.notifications.value
                if (notifs.isNotEmpty()) {
                    focusedNotifIndex = (focusedNotifIndex + 1) % notifs.size
                    Toast.makeText(this, "Foco: ${notifs[focusedNotifIndex].mensaje.take(15)}...", Toast.LENGTH_SHORT).show()
                }
            }
            1 -> { // ORDER SUMMARY: Postpone or Reject order state
                val order = WearStateHolder.activeOrder.value
                if (order != null) {
                    when (order.estado) {
                        "NUEVO" -> {
                            commManager.sendMessage(WearPaths.MSG_POSPONER_PEDIDO, order.id)
                            Toast.makeText(this, "Posponiendo pedido...", Toast.LENGTH_SHORT).show()
                        }
                        "POSPUESTO" -> {
                            commManager.sendMessage(WearPaths.MSG_RECHAZAR_PEDIDO, order.id)
                            Toast.makeText(this, "Rechazando pedido...", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            Toast.makeText(this, "Pedido finalizado o sin acción.", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    Toast.makeText(this, "Sin pedidos activos.", Toast.LENGTH_SHORT).show()
                }
            }
            2 -> { // NEARBY SHOPS: Toggle Favorite status of highlighted shop
                val shops = WearStateHolder.nearbyShops.value
                if (shops.isNotEmpty() && focusedShopIndex in shops.indices) {
                    val shop = shops[focusedShopIndex]
                    commManager.sendMessage(WearPaths.MSG_TOGGLE_FAVORITO, shop.id)
                    Toast.makeText(this, "Marcando favorito: ${shop.nombre}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    /**
     * Loops checking connection and sending pings/heartbeats.
     */
    private fun startHeartbeatLoop() {
        lifecycleScopeLaunch {
            while (true) {
                commManager.checkConnection()
                if (commManager.isConnected.value) {
                    commManager.sendHeartbeat()
                }
                delay(10000) // 10 seconds ping interval
            }
        }
    }

    /**
     * Helper extension to run async inside lifecycleScope.
     */
    private fun lifecycleScopeLaunch(block: suspend () -> Unit) {
        lifecycleScope.launch {
            block()
        }
    }

    /**
     * Triggers short haptic vibration for notifications/alerts.
     */
    private fun triggerHapticFeedback() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(150)
        }
    }
}

/**
 * Proximity Alert Modal Dialog
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

