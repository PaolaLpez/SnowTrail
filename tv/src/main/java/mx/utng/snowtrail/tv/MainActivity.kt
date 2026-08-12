package mx.utng.snowtrail.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxSize
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import mx.utng.snowtrail.tv.database.SnowTrailRepository
import mx.utng.snowtrail.tv.database.SnowTrailRepository.TvOrder
import mx.utng.snowtrail.tv.database.SnowTrailRepository.TvPromotion
import mx.utng.snowtrail.tv.screens.OrdersTvScreen
import mx.utng.snowtrail.tv.screens.PromotionsTvScreen

/**
 * Actividad Principal de Android TV (`:tv`).
 * Actúa como orquestador central que:
 * 1. Inicia el servidor TCP multihilo en el puerto 9090 para recibir comandos del móvil.
 * 2. Procesa los mensajes entrantes: SELECT_SHOP, ADD_PROMO, ADD_ORDER.
 * 3. Navega entre las pantallas declarativas: PromotionsTvScreen y OrdersTvScreen.
 */
class MainActivity : ComponentActivity() {

    private lateinit var repository: SnowTrailRepository
    private var serverJob: kotlinx.coroutines.Job? = null
    private var onSocketMessageReceived: ((String) -> Unit)? = null

    // Mapa de IDs a nombres legibles de sucursales
    private val shopNames = mapOf(
        "nev_los_abuelos" to "Los Abuelos",
        "nev_la_mich" to "La Michoacana",
        "nev_zero" to "Helados Bajo Cero",
        "nev_artis" to "Artesanales del Valle",
        "nev_far" to "Heladería Lejana",
        "nev_centenario" to "Nieves del Centenario",
        "nev_gelato" to "Gelato Italiano",
        "nev_antonio" to "Paletería San Antonio",
        "nev_copo" to "El Copo Dorado",
        "nev_flor" to "Flor de Dolores"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = SnowTrailRepository(applicationContext)

        // Iniciar servidor TCP en segundo plano para recibir comandos del móvil (Puerto 9090)
        serverJob = kotlinx.coroutines.CoroutineScope(Dispatchers.IO).launch {
            var serverSocket: java.net.ServerSocket? = null
            try {
                android.util.Log.d("TvSocketServer", "Iniciando servidor socket en puerto 9090...")
                serverSocket = java.net.ServerSocket(9090)
                android.util.Log.d("TvSocketServer", "Servidor socket iniciado. Esperando conexiones...")
                while (true) {
                    val socket = serverSocket.accept()
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.getInputStream()))
                    val line = reader.readLine()
                    android.util.Log.d("TvSocketServer", "Mensaje recibido del móvil: $line")
                    if (line != null) {
                        withContext(Dispatchers.Main) {
                            onSocketMessageReceived?.invoke(line)
                        }
                    }
                    reader.close()
                    socket.close()
                }
            } catch (e: Exception) {
                android.util.Log.e("TvSocketServer", "Error en servidor socket: ${e.message}", e)
            } finally {
                try { serverSocket?.close() } catch (ex: Exception) {}
            }
        }

        setContent {
            MaterialTheme {
                // Estados de navegación y datos
                var currentScreen by remember { mutableStateOf("promotions") }
                var promotions by remember { mutableStateOf(emptyList<TvPromotion>()) }
                var orders by remember { mutableStateOf(emptyList<TvOrder>()) }
                var selectedShopId by remember { mutableStateOf<String?>("nev_los_abuelos") }

                // Escuchar mensajes TCP y procesar comandos del móvil
                DisposableEffect(Unit) {
                    onSocketMessageReceived = { msg ->
                        try {
                            when {
                                // Comando: cambiar heladería activa en pantalla
                                msg.startsWith("SELECT_SHOP:") -> {
                                    val shopId = msg.substringAfter("SELECT_SHOP:").trim()
                                    selectedShopId = if (shopId == "ALL") null else shopId
                                    val name = if (shopId == "ALL") "Todas" else (shopNames[shopId] ?: shopId)
                                    android.widget.Toast.makeText(applicationContext, "Heladería seleccionada: $name", android.widget.Toast.LENGTH_SHORT).show()
                                }
                                // Comando: nueva promoción enviada desde el móvil
                                msg.startsWith("ADD_PROMO:") -> {
                                    val parts = msg.substringAfter("ADD_PROMO:").split("|")
                                    if (parts.size >= 7) {
                                        val newPromo = TvPromotion(parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim(), parts[5].trim(), parts[6].trim(), parts[0].trim())
                                        repository.savePromotion(newPromo)
                                        promotions = repository.getPromotions()
                                        if (parts[0].trim() == selectedShopId || selectedShopId == null) {
                                            android.widget.Toast.makeText(applicationContext, "⚠️ ¡NUEVA PROMOCIÓN!\n${parts[2].trim()}: ${parts[5].trim()}", android.widget.Toast.LENGTH_LONG).show()
                                            currentScreen = "promotions"
                                        }
                                    }
                                }
                                // Comando: nuevo pedido enviado desde el móvil
                                msg.startsWith("ADD_ORDER:") -> {
                                    val parts = msg.substringAfter("ADD_ORDER:").split("|")
                                    if (parts.size >= 8) {
                                        val newOrder = TvOrder(parts[1].trim(), parts[2].trim(), parts[3].trim(), parts[4].trim(), parts[5].trim(), parts[6].trim(), parts[7].trim(), parts[0].trim())
                                        repository.saveOrder(newOrder)
                                        orders = repository.getOrders()
                                        if (parts[0].trim() == selectedShopId || selectedShopId == null) {
                                            android.widget.Toast.makeText(applicationContext, "🔔 ¡NUEVO PEDIDO!\n${parts[2].trim()}: ${parts[5].trim()}", android.widget.Toast.LENGTH_LONG).show()
                                            currentScreen = "orders"
                                        }
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    onDispose { onSocketMessageReceived = null }
                }

                // Recargar datos desde SQLite al cambiar de pantalla o heladería seleccionada
                LaunchedEffect(currentScreen, selectedShopId) {
                    try {
                        promotions = repository.getPromotions()
                        orders = repository.getOrders()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(applicationContext, "Error DB: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                    }
                }

                val filteredPromotions = promotions.filter { selectedShopId == null || it.neveriaId == selectedShopId }
                val filteredOrders = orders.filter { selectedShopId == null || it.neveriaId == selectedShopId }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFCFAF2)
                ) {
                    // Navegación entre pantallas declarativas
                    if (currentScreen == "promotions") {
                        PromotionsTvScreen(
                            promotions = filteredPromotions,
                            selectedShopName = shopNames[selectedShopId] ?: "Todas",
                            onNavigateToOrders = { currentScreen = "orders" }
                        )
                    } else {
                        OrdersTvScreen(
                            orders = filteredOrders,
                            selectedShopName = shopNames[selectedShopId] ?: "Todas",
                            onUpdateOrder = { orderId, newStatus ->
                                try {
                                    repository.updateOrderStatus(orderId, newStatus)
                                    orders = repository.getOrders()
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(applicationContext, "Error Update: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                }
                            },
                            onBack = { currentScreen = "promotions" }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serverJob?.cancel()
    }
}
