package mx.utng.snowtrail.tv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.foundation.focusable
import androidx.compose.ui.platform.LocalContext
import mx.utng.snowtrail.tv.database.SnowTrailRepository
import mx.utng.snowtrail.tv.database.SnowTrailRepository.TvOrder
import mx.utng.snowtrail.tv.database.SnowTrailRepository.TvPromotion

class MainActivity : ComponentActivity() {
    private lateinit var repository: SnowTrailRepository
    private var serverJob: kotlinx.coroutines.Job? = null
    private var onSocketMessageReceived: ((String) -> Unit)? = null

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

        // Start background TCP socket server to communicate with mobile app
        serverJob = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            var serverSocket: java.net.ServerSocket? = null
            try {
                android.util.Log.d("TvSocketServer", "Iniciando servidor socket en puerto 9090...")
                serverSocket = java.net.ServerSocket(9090)
                android.util.Log.d("TvSocketServer", "Servidor socket iniciado con éxito. Esperando conexiones...")
                while (true) {
                    val socket = serverSocket.accept()
                    android.util.Log.d("TvSocketServer", "Conexión entrante aceptada desde: ${socket.remoteSocketAddress}")
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(socket.getInputStream()))
                    val line = reader.readLine()
                    android.util.Log.d("TvSocketServer", "Mensaje recibido del móvil: $line")
                    if (line != null) {
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            onSocketMessageReceived?.invoke(line)
                        }
                    }
                    reader.close()
                    socket.close()
                }
            } catch (e: Exception) {
                android.util.Log.e("TvSocketServer", "Error fatal en servidor socket: ${e.message}", e)
                e.printStackTrace()
            } finally {
                try { serverSocket?.close() } catch (ex: Exception) {}
            }
        }

        setContent {
            MaterialTheme {
                var currentScreen by remember { mutableStateOf("promotions") } // "promotions" or "orders"
                var promotions by remember { mutableStateOf(emptyList<TvPromotion>()) }
                var orders by remember { mutableStateOf(emptyList<TvOrder>()) }
                var selectedShopId by remember { mutableStateOf<String?>("nev_los_abuelos") }

                // Listen to TCP socket updates
                DisposableEffect(Unit) {
                    onSocketMessageReceived = { msg ->
                        try {
                            if (msg.startsWith("SELECT_SHOP:")) {
                                val shopId = msg.substringAfter("SELECT_SHOP:").trim()
                                selectedShopId = if (shopId == "ALL") null else shopId
                                val name = if (shopId == "ALL") "Todas" else (shopNames[shopId] ?: shopId)
                                android.util.Log.d("TvSocketServer", "SELECT_SHOP recibido: ID=$shopId, Nombre=$name")
                                android.widget.Toast.makeText(applicationContext, "Heladería seleccionada: $name", android.widget.Toast.LENGTH_SHORT).show()
                            } else if (msg.startsWith("ADD_PROMO:")) {
                                val parts = msg.substringAfter("ADD_PROMO:").split("|")
                                if (parts.size >= 7) {
                                    val shopId = parts[0].trim()
                                    val promoId = parts[1].trim()
                                    val nombre = parts[2].trim()
                                    val start = parts[3].trim()
                                    val end = parts[4].trim()
                                    val note = parts[5].trim()
                                    val image = parts[6].trim()

                                    android.util.Log.d("TvSocketServer", "ADD_PROMO recibido: shopId=$shopId, promoId=$promoId, nombre=$nombre")
                                    val newPromo = TvPromotion(promoId, nombre, start, end, note, image, shopId)
                                    repository.savePromotion(newPromo)
                                    promotions = repository.getPromotions() // Reload from DB

                                    if (shopId == selectedShopId || selectedShopId == null) {
                                        android.widget.Toast.makeText(
                                            applicationContext,
                                            "⚠️ ¡NUEVA PROMOCIÓN EN PANTALLA!\n$nombre: $note",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        currentScreen = "promotions" // Go to promotions screen to show it
                                    }
                                }
                            } else if (msg.startsWith("ADD_ORDER:")) {
                                val parts = msg.substringAfter("ADD_ORDER:").split("|")
                                if (parts.size >= 8) {
                                    val shopId = parts[0].trim()
                                    val orderId = parts[1].trim()
                                    val client = parts[2].trim()
                                    val pickup = parts[3].trim()
                                    val eta = parts[4].trim()
                                    val total = parts[5].trim()
                                    val items = parts[6].trim()
                                    val status = parts[7].trim()

                                    android.util.Log.d("TvSocketServer", "ADD_ORDER recibido: shopId=$shopId, orderId=$orderId, client=$client, status=$status")
                                    val newOrder = TvOrder(orderId, client, pickup, eta, total, items, status, shopId)
                                    repository.saveOrder(newOrder)
                                    orders = repository.getOrders() // Reload from DB

                                    if (shopId == selectedShopId || selectedShopId == null) {
                                        android.widget.Toast.makeText(
                                            applicationContext,
                                            "🔔 ¡NUEVO PEDIDO EN ESTA NEVERÍA!\n$client: $total",
                                            android.widget.Toast.LENGTH_LONG
                                        ).show()
                                        currentScreen = "orders" // Go to orders screen to show it
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    }
                    onDispose {
                        onSocketMessageReceived = null
                    }
                }

                // Sync states from DB
                LaunchedEffect(currentScreen, selectedShopId) {
                    try {
                        promotions = repository.getPromotions()
                        orders = repository.getOrders()
                    } catch (e: Exception) {
                        android.widget.Toast.makeText(applicationContext, "Error DB: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                        e.printStackTrace()
                    }
                }

                val filteredPromotions = promotions.filter { selectedShopId == null || it.neveriaId == selectedShopId }
                val filteredOrders = orders.filter { selectedShopId == null || it.neveriaId == selectedShopId }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFFFCFAF2) // Soft vanilla background
                ) {
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
                                    orders = repository.getOrders() // Reload
                                } catch (e: Exception) {
                                    android.widget.Toast.makeText(applicationContext, "Error Update: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                                    e.printStackTrace()
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

@Composable
fun PromotionsTvScreen(
    promotions: List<TvPromotion>,
    selectedShopName: String,
    onNavigateToOrders: () -> Unit
) {
    var currentIndex by remember { mutableIntStateOf(0) }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            delay(100L)
            focusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Auto-scroll loop
    LaunchedEffect(promotions) {
        if (promotions.isNotEmpty()) {
            while (true) {
                delay(4000L)
                currentIndex = (currentIndex + 1) % promotions.size
            }
        }
    }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFE2F9EE), // Mint Green
                        Color(0xFFFCFAF2)  // Vanilla
                    )
                )
            )
            .padding(24.dp)
            .clickable { onNavigateToOrders() }
    ) {
        // Decorative background elements
        Text("🍓", fontSize = 48.sp, modifier = Modifier.align(Alignment.TopEnd).padding(end = 120.dp, top = 20.dp).alpha(0.18f))
        Text("🍦", fontSize = 48.sp, modifier = Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 40.dp).alpha(0.18f))
        Text("🍨", fontSize = 48.sp, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 40.dp, bottom = 40.dp).alpha(0.18f))

        Column(modifier = Modifier.fillMaxSize()) {
            // Header Bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(Color.White, CircleShape)
                            .border(BorderStroke(1.5.dp, Color(0xFFEF9A9A)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍦", fontSize = 32.sp)
                    }
                    Column {
                        Text(
                            text = "LA NIEVERÍA PASTEL",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3E2723)
                        )
                        Text(
                            text = "Heladería Activa: $selectedShopName",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF795548)
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "🔔 NOTIFICACIONES",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF795548)
                    )
                    Text(
                        text = "SUCURSAL MATRIZ",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8F6300)
                    )
                }
            }

            Spacer(modifier = Modifier.height(30.dp))

            Text(
                text = "OPCIÓN DE PROMOCIONES",
                fontSize = 28.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF3E2723),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Promotions list layout
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (promotions.isNotEmpty()) {
                    for (i in -1..1) {
                        val index = (currentIndex + i + promotions.size) % promotions.size
                        val promo = promotions[index]
                        val isCurrent = i == 0

                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) Color.White else Color.White.copy(alpha = 0.6f)
                            ),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(
                                width = if (isCurrent) 3.dp else 1.dp,
                                color = if (isCurrent) Color(0xFFEF9A9A) else Color.LightGray
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isCurrent) 12.dp else 4.dp
                            ),
                            modifier = Modifier
                                .width(if (isCurrent) 340.dp else 260.dp)
                                .height(if (isCurrent) 280.dp else 220.dp)
                                .padding(horizontal = 12.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(if (isCurrent) 90.dp else 65.dp)
                                        .background(Color(0xFFFEE1E8), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(text = promo.imagen, fontSize = if (isCurrent) 45.sp else 32.sp)
                                }

                                Text(
                                    text = promo.nombre,
                                    fontSize = if (isCurrent) 20.sp else 16.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF3E2723),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = promo.nota,
                                    fontSize = if (isCurrent) 12.sp else 10.sp,
                                    color = Color(0xFF795548),
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                } else {
                    Text("No hay promociones activas actualmente.", fontSize = 18.sp, color = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    android.widget.Toast.makeText(context, "Cargando Pedidos...", android.widget.Toast.LENGTH_SHORT).show()
                    onNavigateToOrders()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF9A9A)),
                shape = RoundedCornerShape(20.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .width(220.dp)
                    .height(50.dp)
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🔄 ACTUALIZAR", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun OrdersTvScreen(
    orders: List<TvOrder>,
    selectedShopName: String,
    onUpdateOrder: (String, String) -> Unit,
    onBack: () -> Unit
) {
    val newOrders = orders.filter { it.estado == "NUEVO" }
    val pendingOrders = orders.filter { it.estado == "PENDIENTE" }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        try {
            delay(100L)
            focusRequester.requestFocus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFCFAF2))
            .padding(24.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .background(Color.White, CircleShape)
                            .border(BorderStroke(1.5.dp, Color(0xFFEF9A9A)), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🍦", fontSize = 24.sp)
                    }
                    Column {
                        Text(
                            text = "LA NIEVERÍA PASTEL",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF3E2723)
                        )
                        Text(
                            text = "Heladería Activa: $selectedShopName",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF795548)
                        )
                    }
                }

                Text(
                    text = "PEDIDOS",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF3E2723)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Pedidos Nuevos",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF3E2723),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(Color(0xFFE2F9EE).copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                            .border(BorderStroke(1.5.dp, Color(0xFFE2F9EE)), RoundedCornerShape(16.dp))
                            .padding(16.dp)
                    ) {
                        if (newOrders.isNotEmpty()) {
                            val activeNew = newOrders.first()
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Text(
                                        text = "Cliente: ${activeNew.cliente}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3E2723)
                                    )
                                    Text(
                                        text = activeNew.paraRecoger,
                                        fontSize = 16.sp,
                                        color = Color(0xFF795548)
                                    )
                                    Text(
                                        text = "Tiempo de entrega aprox: ${activeNew.tiempoEntrega}",
                                        fontSize = 16.sp,
                                        color = Color(0xFF795548)
                                    )
                                    Text(
                                        text = "Total: ${activeNew.total}",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFB52D5E)
                                    )

                                    Divider(color = Color.LightGray)

                                    Text(
                                        text = "Items:",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF3E2723)
                                    )
                                    Text(
                                        text = activeNew.items.replace(", ", "\n"),
                                        fontSize = 16.sp,
                                        color = Color(0xFF3E2723)
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Button(
                                        onClick = { onUpdateOrder(activeNew.id, "PENDIENTE") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF81C784)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("✔ Aceptar", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onUpdateOrder(activeNew.id, "PENDIENTE") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFD54F)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("🕒 Posponer", color = Color(0xFF5D4037), fontWeight = FontWeight.Bold)
                                    }
                                    Button(
                                        onClick = { onUpdateOrder(activeNew.id, "RECHAZADO") },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373)),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("❌ Rechazar", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No hay nuevos pedidos.", fontSize = 16.sp, color = Color.Gray)
                            }
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    Text(
                        text = "Pendientes",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF3E2723),
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (pendingOrders.isNotEmpty()) {
                            items(pendingOrders) { order ->
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = Color.White),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, Color.LightGray),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(modifier = Modifier.padding(16.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column {
                                                Text(
                                                    text = "# Num. Pedido: ${order.id}",
                                                    fontSize = 16.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF3E2723)
                                                )
                                                Text(
                                                    text = "Cliente: ${order.cliente}",
                                                    fontSize = 14.sp,
                                                    color = Color.Gray
                                                )
                                            }

                                            Button(
                                                onClick = { onUpdateOrder(order.id, "ENTREGADO") },
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD)),
                                                shape = RoundedCornerShape(10.dp),
                                                border = BorderStroke(1.dp, Color(0xFF1565C0)),
                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                                                modifier = Modifier.height(36.dp)
                                            ) {
                                                Text("✔ Entregado", color = Color(0xFF1565C0), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                            }
                                        }

                                        Spacer(modifier = Modifier.height(8.dp))
                                        Text(
                                            text = "Items:\n${order.items.replace(", ", "\n")}",
                                            fontSize = 13.sp,
                                            color = Color.DarkGray
                                        )
                                    }
                                }
                            }
                        } else {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(150.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("No hay pedidos pendientes.", fontSize = 16.sp, color = Color.Gray)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    android.widget.Toast.makeText(context, "Volviendo a Promociones...", android.widget.Toast.LENGTH_SHORT).show()
                    onBack()
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA7B8C4)),
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .width(180.dp)
                    .height(45.dp)
                    .focusRequester(focusRequester)
                    .focusable()
            ) {
                Text("🔄 Actualizar", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}
