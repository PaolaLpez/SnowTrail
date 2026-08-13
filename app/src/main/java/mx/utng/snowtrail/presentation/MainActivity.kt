package mx.utng.snowtrail.presentation

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import mx.utng.snowtrail.database.SnowTrailRepository
import mx.utng.snowtrail.presentation.screens.AdminPanelScreen
import mx.utng.snowtrail.presentation.screens.CatalogoScreen
import mx.utng.snowtrail.presentation.screens.NeveriasScreen
import mx.utng.snowtrail.presentation.screens.PedidoActivoScreen
import mx.utng.snowtrail.presentation.theme.MobileThemeColors
import mx.utng.snowtrail.presentation.theme.SnowTrailTheme
import mx.utng.snowtrail.service.MockOrder
import mx.utng.snowtrail.service.MockProductLine
import mx.utng.snowtrail.service.WearSyncService

/**
 * ARCHIVO: MainActivity.kt
 * PROPÓSITO: Actividad Principal de Presentación (UI Layer).
 * Orquesta la navegación entre pantallas declarativas en la aplicación móvil:
 * 1. Explorador de Neverías (Todas / Favoritas)
 * 2. Catálogo de Nieves y Carrito de Compras
 * 3. Seguimiento de Pedido Activo (Ticket)
 * 4. Panel de Administración (Gestión de estados 2x2)
 */
class MainActivity : ComponentActivity() {

    private lateinit var repository: SnowTrailRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        repository = SnowTrailRepository(this)

        try {
            setContent {
                SnowTrailTheme {
                    SnowTrailMainScreen(
                        repository = repository,
                        onNotifyStateChange = { action ->
                            Toast.makeText(this, "Estado sincronizado: $action", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(this, "Error al iniciar: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SnowTrailMainScreen(
    repository: SnowTrailRepository,
    onNotifyStateChange: (String) -> Unit
) {
    var userRole by remember { mutableStateOf("CLIENTE") }
    var currentTab by remember { mutableIntStateOf(0) }
    var showOnlyFavorites by remember { mutableStateOf(false) }

    val shops = remember { mutableStateListOf(*WearSyncService.mockShops.toTypedArray()) }
    var activeOrder by remember { mutableStateOf(WearSyncService.activeOrderState) }

    val cartItems = remember {
        mutableStateListOf(
            Pair(MockProductLine("Copa Helarte Suprema", 1, 95.0), true),
            Pair(MockProductLine("Nieve Artesanal de Limón", 2, 45.0), false)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "SnowTrail",
                            fontWeight = FontWeight.Bold,
                            color = MobileThemeColors.PinkText,
                            fontSize = 20.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        AssistChip(
                            onClick = {
                                userRole = if (userRole == "CLIENTE") "ADMIN" else "CLIENTE"
                            },
                            label = { Text("Modo: $userRole", fontSize = 11.sp) },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (userRole == "ADMIN") Icons.Default.AdminPanelSettings else Icons.Default.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = MobileThemeColors.PinkText
                                )
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MobileThemeColors.OffWhiteVanilla
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = { Icon(Icons.Default.Storefront, contentDescription = "Neverías") },
                    label = { Text("Neverías", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.ShoppingCart, contentDescription = "Catálogo") },
                    label = { Text("Catálogo", fontSize = 11.sp) }
                )
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = {
                        BadgedBox(
                            badge = {
                                if (activeOrder != null) {
                                    Badge { Text("1") }
                                }
                            }
                        ) {
                            Icon(Icons.Default.ReceiptLong, contentDescription = "Pedido")
                        }
                    },
                    label = { Text("Mi Pedido", fontSize = 11.sp) }
                )
                if (userRole == "ADMIN") {
                    NavigationBarItem(
                        selected = currentTab == 3,
                        onClick = { currentTab = 3 },
                        icon = { Icon(Icons.Default.Dashboard, contentDescription = "Admin") },
                        label = { Text("Admin", fontSize = 11.sp) }
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MobileThemeColors.OffWhiteVanilla)
        ) {
            // [MOTOR DE RUTAS DECLARATIVO]: Conmutador de pantallas reactivo según el índice currentTab sin necesidad de NavController XML
            when (currentTab) {
                0 -> NeveriasScreen(
                    shops = shops,
                    showFavoritesOnly = showOnlyFavorites,
                    onToggleFilter = { showOnlyFavorites = !showOnlyFavorites },
                    onToggleFavorite = { shopId ->
                        val index = shops.indexOfFirst { it.id == shopId }
                        if (index != -1) {
                            val shop = shops[index]
                            val newFav = !shop.esFavorita
                            shops[index] = shop.copy(esFavorita = newFav)
                            val mockIdx = WearSyncService.mockShops.indexOfFirst { it.id == shopId }
                            if (mockIdx != -1) {
                                WearSyncService.mockShops[mockIdx].esFavorita = newFav
                            }
                            // [PERSISTENCIA RELACIONAL]: Actualiza la tabla puente M:N user_favorites en SQLite local
                            repository.toggleFavoriteShopForUser("cliente@snowtrail.com", shopId)
                        }
                    }
                )
                1 -> CatalogoScreen(
                    onAddToCart = { item -> cartItems.add(Pair(item, false)) },
                    onCheckout = {
                        val subtotal = cartItems.sumOf { it.first.cantidad * it.first.precioUnitario }
                        val newOrder = MockOrder(
                            id = "order_${System.currentTimeMillis().toString().takeLast(4)}",
                            neveriaId = "nev_los_abuelos",
                            neveriaNombre = "Los Abuelos",
                            estado = "NUEVO",
                            tiempoEstimadoMinutos = 20,
                            fechaHoraMillis = System.currentTimeMillis(),
                            total = subtotal,
                            productos = cartItems.map { it.first },
                            userEmail = "cliente@snowtrail.com"
                        )
                        activeOrder = newOrder
                        WearSyncService.activeOrderState = newOrder
                        // [TRANSACCIÓN ACID]: Guarda cabecera y detalle de orden en SQLite mediante transacciones atómicas
                        repository.saveOrder(newOrder)
                        onNotifyStateChange("Nuevo Pedido Creado")
                        currentTab = 2
                    }
                )

                2 -> PedidoActivoScreen(
                    order = activeOrder,
                    onSimulateProgress = { nextState ->
                        activeOrder?.let {
                            it.estado = nextState
                            WearSyncService.activeOrderState?.estado = nextState
                            activeOrder = it.copy(estado = nextState)
                            // [MÁQUINA DE ESTADOS]: Transición de estado del pedido (NUEVO -> ACEPTADO -> ENTREGADO)
                            repository.updateOrderStatus(it.id, nextState)
                            onNotifyStateChange("Pedido -> $nextState")
                        }
                    }
                )
                3 -> AdminPanelScreen(
                    activeOrder = activeOrder,
                    onUpdateState = { newState ->
                        activeOrder?.let {
                            it.estado = newState
                            WearSyncService.activeOrderState?.estado = newState
                            activeOrder = it.copy(estado = newState)
                            // [PANEL DE COCINA/ADMIN]: Actualiza el estatus del pedido desde la consola de administración 2x2
                            repository.updateOrderStatus(it.id, newState)
                            onNotifyStateChange("Admin actualizó a $newState")
                        }
                    }
                )
            }
        }
    }
}