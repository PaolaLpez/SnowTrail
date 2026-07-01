package mx.utng.snowtrail

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import mx.utng.snowtrail.database.SnowTrailRepository
import mx.utng.snowtrail.service.MockNotification
import mx.utng.snowtrail.service.MockOrder
import mx.utng.snowtrail.service.MockProductLine
import mx.utng.snowtrail.service.MockShop
import mx.utng.snowtrail.service.WearSyncService
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

// Creamy Pastel Ice Cream Color System
object MobileThemeColors {
    val OffWhiteVanilla = Color(0xFFFCFAF2)    // Warm soft vanilla cream background
    val PureWhiteCard = Color(0xFFFFFFFF)      // Cards background
    
    // Pastel Accents
    val IceCreamPink = Color(0xFFFEE1E8)       // Strawberry Pink
    val PinkText = Color(0xFFB52D5E)
    
    val IceCreamMint = Color(0xFFE2F9EE)       // Mint Green
    val MintText = Color(0xFF1E6F40)
    
    val IceCreamPeach = Color(0xFFFFEAE2)      // Warm Peach Orange
    val PeachText = Color(0xFFBF3E15)
    
    val IceCreamLavender = Color(0xFFECEBFF)   // Lavender Blue
    val LavenderText = Color(0xFF4A34AC)
    
    val GoldPastel = Color(0xFFFFF0C2)         // Creamy Honey Yellow
    val GoldText = Color(0xFF8F6300)
    val GoldBorder = Color(0xFFFFD54F)
    
    // Cocoa Typography
    val CocoaDarkText = Color(0xFF3E2723)      // Dark chocolate brown for main text
    val CocoaLightText = Color(0xFF795548)     // Soft milk chocolate for secondary text
    val CocoaMuted = Color(0xFFA1887F)         // Muted brown
    
    // Order Status Capsule Colors (Pastel-themed)
    val NuevoBg = Color(0xFFFFF9C4)
    val NuevoText = Color(0xFFF57F17)
    
    val AceptadoBg = Color(0xFFE8F5E9)
    val AceptadoText = Color(0xFF2E7D32)
    
    val PospuestoBg = Color(0xFFFFE0B2)
    val PospuestoText = Color(0xFFE65100)
    
    val RechazadoBg = Color(0xFFFFEBEE)
    val RechazadoText = Color(0xFFC62828)
    
    val EntregadoBg = Color(0xFFE3F2FD)
    val EntregadoText = Color(0xFF1565C0)
}

class MainActivity : ComponentActivity() {

    private lateinit var repository: SnowTrailRepository
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var onLocationChangedCallback: ((Double, Double) -> Unit)? = null

    fun startLocationUpdates(context: Context) {
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            try {
                locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                locationListener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        onLocationChangedCallback?.invoke(location.latitude, location.longitude)
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    3000L,
                    3f,
                    locationListener!!
                )
                locationManager?.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER,
                    3000L,
                    3f,
                    locationListener!!
                )

                val lastKnown = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                    ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
                lastKnown?.let {
                    onLocationChangedCallback?.invoke(it.latitude, it.longitude)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stopLocationUpdates() {
        locationListener?.let { locationManager?.removeUpdates(it) }
        locationListener = null
        locationManager = null
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        repository = SnowTrailRepository(applicationContext)
        repository.initializeDemoDataIfEmpty()

        // Start Wear Sync Service to initialize DataLayer
        val startIntent = Intent(this, WearSyncService::class.java).apply {
            action = "mx.utng.snowtrail.ACTION_SYNC_ALL"
        }
        startService(startIntent)

        setContent {
            // Navigation states: "home", "shop_detail", "route_navigation"
            var currentScreen by remember { mutableStateOf("home") }
            var selectedShopId by remember { mutableStateOf("") }
            var selectedTab by remember { mutableIntStateOf(0) }
            
            // Observe states dynamically from SQLite database
            var activeOrder by remember { mutableStateOf<MockOrder?>(null) }
            var shopsList by remember { mutableStateOf<List<MockShop>>(emptyList()) }
            var notificationsList by remember { mutableStateOf<List<MockNotification>>(emptyList()) }

            // Helper to reload from database
            val reloadFromDb = {
                activeOrder = repository.getActiveOrder()
                shopsList = repository.getShops()
                notificationsList = repository.getNotifications()
            }

            // Sync triggers and UI update
            val triggerSync = { actionName: String, extraKey: String?, extraVal: String? ->
                val intent = Intent(this, WearSyncService::class.java).apply {
                    action = actionName
                    if (extraKey != null && extraVal != null) {
                        putExtra(extraKey, extraVal)
                    }
                }
                startService(intent)
                
                // Allow background task to sync then reload from DB
                reloadFromDb()
            }

            // Location States for GPS tracking
            var useRealGps by remember { mutableStateOf(false) }
            var userLatitude by remember { mutableDoubleStateOf(21.1561) }
            var userLongitude by remember { mutableDoubleStateOf(-100.9312) }

            // LaunchedEffect to manage location listener updates
            LaunchedEffect(useRealGps) {
                if (useRealGps) {
                    onLocationChangedCallback = { lat, lng ->
                        userLatitude = lat
                        userLongitude = lng
                        
                        val shopCoords = mapOf(
                             "nev_los_abuelos" to Pair(21.1565, -100.9310),
                             "nev_la_mich" to Pair(21.1590, -100.9300),
                             "nev_zero" to Pair(21.1660, -100.9250),
                             "nev_artis" to Pair(21.1780, -100.9150),
                             "nev_far" to Pair(21.1960, -100.9000),
                             "nev_centenario" to Pair(21.1400, -100.9500),
                             "nev_gelato" to Pair(21.1850, -100.9100),
                             "nev_antonio" to Pair(21.1650, -100.9450),
                             "nev_copo" to Pair(21.1350, -100.9200),
                             "nev_flor" to Pair(21.2100, -100.8800)
                        )
                        shopCoords.forEach { (id, coords) ->
                            val results = FloatArray(1)
                            try {
                                Location.distanceBetween(lat, lng, coords.first, coords.second, results)
                                val distance = results[0].toDouble()
                                repository.updateShopDistance(id, distance)
                            } catch (e: Exception) {
                                val r = 6371e3
                                val phi1 = Math.toRadians(lat)
                                val phi2 = Math.toRadians(coords.first)
                                val deltaPhi = Math.toRadians(coords.first - lat)
                                val deltaLambda = Math.toRadians(coords.second - lng)
                                val a = Math.sin(deltaPhi / 2) * Math.sin(deltaPhi / 2) +
                                        Math.cos(phi1) * Math.cos(phi2) *
                                        Math.sin(deltaLambda / 2) * Math.sin(deltaLambda / 2)
                                val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
                                repository.updateShopDistance(id, r * c)
                            }
                        }
                        
                        reloadFromDb()
                        triggerSync("mx.utng.snowtrail.ACTION_SYNC_SHOPS", null, null)
                    }
                    startLocationUpdates(applicationContext)
                } else {
                    stopLocationUpdates()
                    onLocationChangedCallback = null
                }
            }

            // Load initial states
            LaunchedEffect(Unit) {
                reloadFromDb()
            }

            // Handle back press
            BackHandler(enabled = currentScreen != "home") {
                currentScreen = "home"
                reloadFromDb()
            }

            // Listen to intent target navigation from watch clicks
            LaunchedEffect(intent) {
                val target = intent.getStringExtra("navigation_target")
                if (target == "shop_detail") {
                    selectedShopId = intent.getStringExtra("shop_id") ?: ""
                    if (selectedShopId.isNotEmpty()) {
                        currentScreen = "shop_detail"
                    }
                } else if (target == "notification") {
                    selectedTab = 2 // Go to simulation / dashboard tray
                    currentScreen = "home"
                }
            }

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MobileThemeColors.OffWhiteVanilla
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Ice Cream Header Bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(
                                        MobileThemeColors.IceCreamPink,
                                        MobileThemeColors.IceCreamPeach,
                                        MobileThemeColors.IceCreamMint
                                    )
                                ),
                                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                            )
                            .border(
                                BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                                shape = RoundedCornerShape(bottomStart = 28.dp, bottomEnd = 28.dp)
                            )
                            .padding(top = 16.dp, bottom = 20.dp, start = 20.dp, end = 20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (currentScreen != "home") {
                                    IconButton(
                                        onClick = { 
                                            currentScreen = "home"
                                            reloadFromDb()
                                        },
                                        modifier = Modifier
                                            .padding(end = 8.dp)
                                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(50))
                                            .size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ArrowBack,
                                            contentDescription = "Volver",
                                            tint = MobileThemeColors.PinkText,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                                Column {
                                    Text(
                                        text = when (currentScreen) {
                                            "shop_detail" -> "Detalle de Nevería"
                                            "route_navigation" -> "Navegación al Local"
                                            else -> "SnowTrail"
                                        },
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = MobileThemeColors.CocoaDarkText
                                    )
                                    Text(
                                        text = when (currentScreen) {
                                            "shop_detail" -> "Explora sabores y pide tu helado"
                                            "route_navigation" -> "Simulador de ruta en tiempo real"
                                            else -> "Tu compañero dulce de heladerías"
                                        },
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MobileThemeColors.CocoaLightText
                                    )
                                }
                            }
                            
                            // Connected badge styled like an ice cream capsule
                            Box(
                                modifier = Modifier
                                    .background(Color.White, RoundedCornerShape(14.dp))
                                    .border(BorderStroke(1.5.dp, MobileThemeColors.IceCreamMint), RoundedCornerShape(14.dp))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(MobileThemeColors.MintText, RoundedCornerShape(50))
                                    ) {
                                        Text(
                                            text = "Listo para Pedir",
                                            color = MobileThemeColors.MintText,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Main screen content based on navigation
                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                        when (currentScreen) {
                            "home" -> Column(modifier = Modifier.fillMaxSize()) {
                                // Rounded Floating Tab Navigation Bar
                                TabRow(
                                    selectedTabIndex = selectedTab,
                                    containerColor = Color.Transparent,
                                    contentColor = MobileThemeColors.PinkText,
                                    indicator = { tabPositions ->
                                        TabRowDefaults.SecondaryIndicator(
                                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                            color = MobileThemeColors.PinkText
                                        )
                                    },
                                    modifier = Modifier
                                        .padding(horizontal = 16.dp, vertical = 8.dp)
                                        .background(Color.White, RoundedCornerShape(16.dp))
                                        .border(BorderStroke(1.dp, MobileThemeColors.IceCreamPeach.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                                        .clip(RoundedCornerShape(16.dp))
                                ) {
                                    Tab(
                                        selected = selectedTab == 0,
                                        onClick = { 
                                            selectedTab = 0 
                                            reloadFromDb()
                                        },
                                        text = { Text("Explorar", fontWeight = FontWeight.Bold) },
                                        icon = { Icon(Icons.Default.Explore, contentDescription = "Explorar") }
                                    )
                                    Tab(
                                        selected = selectedTab == 1,
                                        onClick = { 
                                            selectedTab = 1 
                                            reloadFromDb()
                                        },
                                        text = { Text("Mi Pedido", fontWeight = FontWeight.Bold) },
                                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Pedido") }
                                    )
                                    Tab(
                                        selected = selectedTab == 2,
                                        onClick = { 
                                            selectedTab = 2 
                                            reloadFromDb()
                                        },
                                        text = { Text("Simular", fontWeight = FontWeight.Bold) },
                                        icon = { Icon(Icons.Default.Settings, contentDescription = "Simulador") }
                                    )
                                    Tab(
                                        selected = selectedTab == 3,
                                        onClick = { 
                                            selectedTab = 3 
                                            reloadFromDb()
                                        },
                                        text = { Text("GPS", fontWeight = FontWeight.Bold) },
                                        icon = { Icon(Icons.Default.Place, contentDescription = "GPS") }
                                    )
                                }

                                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                    when (selectedTab) {
                                        0 -> ExploreShopsScreen(
                                            shops = shopsList,
                                            onToggleFavorite = { shopId ->
                                                repository.toggleFavoriteShop(shopId)
                                                reloadFromDb()
                                                // Sync updated shops list with Wear OS immediately
                                                triggerSync("mx.utng.snowtrail.ACTION_SYNC_SHOPS", null, null)
                                            },
                                            onShopClicked = { shopId ->
                                                selectedShopId = shopId
                                                currentScreen = "shop_detail"
                                            }
                                        )
                                        1 -> MobileOrderScreen(
                                            order = activeOrder,
                                            onNavigateToRoute = {
                                                currentScreen = "route_navigation"
                                            }
                                        )
                                        2 -> SimulatorControlScreen(
                                            order = activeOrder,
                                            notifications = notificationsList,
                                            onActionTriggered = { actionName, extraKey, extraVal ->
                                                triggerSync(actionName, extraKey, extraVal)
                                                reloadFromDb()
                                            }
                                        )
                                        3 -> GPSScreen(
                                            useRealGps = useRealGps,
                                            userLat = userLatitude,
                                            userLng = userLongitude,
                                            onToggleRealGps = { enabled ->
                                                useRealGps = enabled
                                                if (!enabled) {
                                                    userLatitude = 21.1561
                                                    userLongitude = -100.9312
                                                    val basePositions = mapOf(
                                                        "nev_los_abuelos" to 80.0,
                                                        "nev_la_mich" to 350.0,
                                                        "nev_zero" to 1200.0,
                                                        "nev_artis" to 2900.0,
                                                        "nev_far" to 4500.0,
                                                        "nev_centenario" to 2800.0,
                                                        "nev_gelato" to 3800.0,
                                                        "nev_antonio" to 1800.0,
                                                        "nev_copo" to 2600.0,
                                                        "nev_flor" to 8000.0
                                                    )
                                                    basePositions.forEach { (id, basePos) ->
                                                         repository.updateShopDistance(id, basePos)
                                                    }
                                                    triggerSync("mx.utng.snowtrail.ACTION_SYNC_SHOPS", null, null)
                                                }
                                            },
                                            onGPSMoved = { sliderValue ->
                                                val simulatedBasePositions = mapOf(
                                                    "nev_los_abuelos" to 80.0,
                                                    "nev_la_mich" to 350.0,
                                                    "nev_zero" to 1200.0,
                                                    "nev_artis" to 2900.0,
                                                    "nev_far" to 4500.0,
                                                    "nev_centenario" to 2800.0,
                                                    "nev_gelato" to 3800.0,
                                                    "nev_antonio" to 1800.0,
                                                    "nev_copo" to 2600.0,
                                                    "nev_flor" to 8000.0
                                                )
                                                simulatedBasePositions.forEach { (id, basePos) ->
                                                    val newDistance = kotlin.math.abs(sliderValue.toDouble() - basePos)
                                                    repository.updateShopDistance(id, newDistance)
                                                }
                                                triggerSync("mx.utng.snowtrail.ACTION_SYNC_SHOPS", null, null)
                                            }
                                        )
                                    }
                                }
                            }
                            "shop_detail" -> ShopDetailScreen(
                                shopId = selectedShopId,
                                shops = shopsList,
                                repository = repository,
                                onOrderCreated = {
                                    triggerSync("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                                    selectedTab = 1
                                    currentScreen = "home"
                                }
                            )
                            "route_navigation" -> RouteNavigationScreen(
                                order = activeOrder,
                                shops = shopsList,
                                onSimulateProximity = { shopId ->
                                    triggerSync("mx.utng.snowtrail.ACTION_TRIGGER_PROXIMITY", "shop_id", shopId)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::repository.isInitialized) {
            val startIntent = Intent(this, WearSyncService::class.java).apply {
                action = "mx.utng.snowtrail.ACTION_SYNC_ALL"
            }
            startService(startIntent)
        }
    }
}

/**
 * SCREEN 1: EXPLORE SHOPS LIST
 */
@Composable
fun ExploreShopsScreen(
    shops: List<MockShop>,
    onToggleFavorite: (String) -> Unit,
    onShopClicked: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    
    // Only display shops within the 3 km (3000 meters) range
    val filteredShops = shops.filter { 
        it.distancia <= 3000.0 && it.nombre.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("¿Qué sabor o paleta se te antoja hoy?...", color = MobileThemeColors.CocoaMuted) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar", tint = MobileThemeColors.PinkText) },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MobileThemeColors.IceCreamPink,
                unfocusedBorderColor = Color.White,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedTextColor = MobileThemeColors.CocoaDarkText,
                unfocusedTextColor = MobileThemeColors.CocoaDarkText
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(BorderStroke(1.5.dp, MobileThemeColors.IceCreamPink), RoundedCornerShape(16.dp))
        )

        Text(
            text = "Neverías Cerca de Ti (${filteredShops.size} encontradas)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MobileThemeColors.CocoaLightText
        )

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(filteredShops) { shop ->
                val cardBorderColor = if (shop.esFavorita) MobileThemeColors.GoldBorder else MobileThemeColors.IceCreamPink
                val cardBg = if (shop.esFavorita) MobileThemeColors.GoldPastel.copy(alpha = 0.3f) else Color.White

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.5.dp, cardBorderColor),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onShopClicked(shop.id) }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = shop.nombre,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MobileThemeColors.CocoaDarkText
                                )
                                if (shop.tienePromocion) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Box(
                                        modifier = Modifier
                                            .background(MobileThemeColors.GoldPastel, RoundedCornerShape(6.dp))
                                            .border(BorderStroke(1.dp, MobileThemeColors.GoldBorder), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "PROMO",
                                            color = MobileThemeColors.GoldText,
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(Icons.Default.Place, contentDescription = "Ubicación", tint = MobileThemeColors.PinkText, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "Distancia: ${shop.distancia.toInt()} metros",
                                    fontSize = 12.sp,
                                    color = MobileThemeColors.CocoaLightText
                                )
                            }
                            
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(top = 2.dp)
                            ) {
                                Icon(Icons.Default.AccessTime, contentDescription = "Horario", tint = MobileThemeColors.CocoaMuted, modifier = Modifier.size(12.dp))
                                Text(
                                    text = "Abierto de 9:00 AM a 9:00 PM",
                                    fontSize = 11.sp,
                                    color = MobileThemeColors.CocoaMuted
                                )
                            }
                        }

                        IconButton(
                            onClick = { onToggleFavorite(shop.id) },
                            modifier = Modifier
                                .background(if (shop.esFavorita) MobileThemeColors.GoldPastel else Color.Transparent, RoundedCornerShape(50))
                        ) {
                            Icon(
                                imageVector = if (shop.esFavorita) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Favorito",
                                tint = if (shop.esFavorita) MobileThemeColors.GoldText else MobileThemeColors.CocoaMuted,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 2: MOBILE ORDER SUMMARY SCREEN
 */
@Composable
fun MobileOrderScreen(
    order: MockOrder?,
    onNavigateToRoute: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        if (order != null) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(2.dp, MobileThemeColors.IceCreamPink),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Ticket de Pedido",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MobileThemeColors.CocoaDarkText
                        )
                        
                        val (badgeCol, textCol) = when (order.estado) {
                            "NUEVO" -> Pair(MobileThemeColors.NuevoBg, MobileThemeColors.NuevoText)
                            "ACEPTADO" -> Pair(MobileThemeColors.AceptadoBg, MobileThemeColors.AceptadoText)
                            "POSPUESTO" -> Pair(MobileThemeColors.PospuestoBg, MobileThemeColors.PospuestoText)
                            "RECHAZADO" -> Pair(MobileThemeColors.RechazadoBg, MobileThemeColors.RechazadoText)
                            "ENTREGADO" -> Pair(MobileThemeColors.EntregadoBg, MobileThemeColors.EntregadoText)
                            else -> Pair(MobileThemeColors.IceCreamMint, MobileThemeColors.MintText)
                        }
                        
                        Box(
                            modifier = Modifier
                                .background(badgeCol, RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, textCol), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = order.estado,
                                color = textCol,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Divider(color = MobileThemeColors.IceCreamPink.copy(alpha = 0.5f), thickness = 1.5.dp)

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(Icons.Default.Store, contentDescription = "Heladería", tint = MobileThemeColors.PinkText)
                        Text(
                            text = order.neveriaNombre,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = MobileThemeColors.CocoaDarkText
                        )
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .background(MobileThemeColors.OffWhiteVanilla, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) {
                        order.productos.forEach { prod ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "${prod.cantidad}x ${prod.nombre}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MobileThemeColors.CocoaLightText
                                )
                                Text(
                                    text = "$${prod.cantidad * prod.precioUnitario}",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MobileThemeColors.CocoaDarkText
                                )
                            }
                        }
                    }

                    Divider(color = MobileThemeColors.IceCreamPink.copy(alpha = 0.5f), thickness = 1.dp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Total a Pagar",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MobileThemeColors.CocoaDarkText
                        )
                        Text(
                            text = "$${order.total}",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = MobileThemeColors.PinkText
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Button to open Map and Directions Screen
                    Button(
                        onClick = onNavigateToRoute,
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.PinkText),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Navigation, contentDescription = "Mapa", tint = Color.White)
                            Text("Ver Ruta y Mapa Pastel de Helados", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxSize()
            ) {
                Icon(Icons.Default.Icecream, contentDescription = "Sin pedidos", tint = MobileThemeColors.CocoaMuted, modifier = Modifier.size(56.dp))
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No tienes ningún pedido activo.\n¡Ve a Explorar y saborea un delicioso helado!",
                    color = MobileThemeColors.CocoaLightText,
                    textAlign = TextAlign.Center,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

/**
 * SCREEN 3: DETALLE DE NEVERÍA (CATALOG AND CART SIMULATION)
 */
@Composable
fun ShopDetailScreen(
    shopId: String,
    shops: List<MockShop>,
    repository: SnowTrailRepository,
    onOrderCreated: () -> Unit
) {
    val shop = shops.find { it.id == shopId } ?: return
    
    // Hardcoded products catalog matching the shop
    val catalog = listOf(
        CatalogItem("Nieve de Guanábana Especial", "Nieve", "Sabor clásico refrescante", 45.0),
        CatalogItem("Helado de Chocolate Belga", "Helado", "Sabor cremoso e intenso", 60.0),
        CatalogItem("Paleta de Fresas con Crema", "Paleta", "Con trozos naturales de fruta", 35.0),
        CatalogItem("Nieve de Limón con Chía", "Nieve", "Deliciosa y muy refrescante", 40.0),
        CatalogItem("Helado de Pistache Premium", "Helado", "Cremoso con trocitos tostados", 65.0)
    )

    val cart = remember { mutableStateListOf<CartLine>() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Shop info card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, MobileThemeColors.IceCreamPeach),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {},
                    modifier = Modifier.background(MobileThemeColors.IceCreamPeach, RoundedCornerShape(50))
                ) {
                    Icon(
                        Icons.Default.Storefront,
                        contentDescription = "Tienda",
                        tint = MobileThemeColors.PeachText,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(shop.nombre, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MobileThemeColors.CocoaDarkText)
                    Text("Distancia: ${shop.distancia.toInt()} metros • Abierto ahora", fontSize = 12.sp, color = MobileThemeColors.CocoaLightText)
                }
            }
        }

        Text("SABORES Y PRODUCTOS DISPONIBLES", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MobileThemeColors.PinkText)

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(catalog) { product ->
                // Colorful theme based on product type
                val (itemBg, itemBorder, itemTextCol) = when (product.tipo) {
                    "Nieve" -> Triple(MobileThemeColors.IceCreamMint, MobileThemeColors.IceCreamMint, MobileThemeColors.MintText)
                    "Helado" -> Triple(MobileThemeColors.IceCreamLavender, MobileThemeColors.IceCreamLavender, MobileThemeColors.LavenderText)
                    "Paleta" -> Triple(MobileThemeColors.IceCreamPink, MobileThemeColors.IceCreamPink, MobileThemeColors.PinkText)
                    else -> Triple(Color.White, MobileThemeColors.IceCreamPeach, MobileThemeColors.PeachText)
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.5.dp, itemBorder),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .background(itemBg, RoundedCornerShape(6.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = product.tipo.uppercase(),
                                        color = itemTextCol,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = product.nombre,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MobileThemeColors.CocoaDarkText
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(product.nota, fontSize = 11.sp, color = MobileThemeColors.CocoaLightText)
                            Text("$${product.precio}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MobileThemeColors.PinkText)
                        }

                        Button(
                            onClick = {
                                val existingIndex = cart.indexOfFirst { it.item.nombre == product.nombre }
                                if (existingIndex != -1) {
                                    cart[existingIndex] = cart[existingIndex].copy(qty = cart[existingIndex].qty + 1)
                                } else {
                                    cart.add(CartLine(product, 1))
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = itemBg),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text("+ Añadir", color = itemTextCol, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // Cart Summary and Confirm Button
        if (cart.isNotEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(2.dp, MobileThemeColors.IceCreamMint),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(Icons.Default.ShoppingBasket, contentDescription = "Carrito", tint = MobileThemeColors.MintText)
                        Text("Detalle de tu Compra", fontWeight = FontWeight.Bold, color = MobileThemeColors.CocoaDarkText, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    val total = cart.sumOf { it.qty * it.item.precio }
                    
                    cart.forEach { line ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)
                        ) {
                            Text("${line.qty}x ${line.item.nombre}", fontSize = 13.sp, color = MobileThemeColors.CocoaLightText)
                            Text("$${line.qty * line.item.precio}", fontSize = 13.sp, color = MobileThemeColors.CocoaDarkText)
                        }
                    }

                    Divider(color = MobileThemeColors.IceCreamMint, modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Total del Pedido", fontWeight = FontWeight.Bold, color = MobileThemeColors.CocoaDarkText, fontSize = 14.sp)
                        Text("$${total}", fontWeight = FontWeight.ExtraBold, color = MobileThemeColors.MintText, fontSize = 18.sp)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Button(
                        onClick = {
                            val newOrder = MockOrder(
                                id = UUID.randomUUID().toString().take(8),
                                neveriaId = shop.id,
                                neveriaNombre = shop.nombre,
                                estado = "NUEVO",
                                tiempoEstimadoMinutos = 15,
                                fechaHoraMillis = System.currentTimeMillis(),
                                total = total,
                                productos = cart.map { MockProductLine(it.item.nombre, it.qty, it.item.precio) }
                            )
                            // Save order in SQLite DB directly
                            repository.saveOrder(newOrder)
                            onOrderCreated()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.MintText),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Confirmar y Enviar Pedido ($${total})", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 4: ROUTE NAVIGATION SCREEN WITH CANVAS DRAWN MAP AND STEP-BY-STEP DIRECTIONS
 */
@Composable
fun RouteNavigationScreen(
    order: MockOrder?,
    shops: List<MockShop>,
    onSimulateProximity: (String) -> Unit
) {
    if (order == null) return
    val activeShop = shops.find { it.id == order.neveriaId }
    val maxDistance = activeShop?.distancia ?: 350.0

    // Simulation states
    var simulationProgress by remember { mutableStateOf(0.0f) }
    var isSimulating by remember { mutableStateOf(false) }
    
    // Calculate current distance dynamically
    val currentDistance = (maxDistance * (1.0f - simulationProgress)).toInt()

    // Step-by-step navigation instructions
    val directions = listOf(
        NavigationStep("Sal del punto de partida y camina 50m por Avenida Vainilla Cream.", 0.0f..0.2f),
        NavigationStep("Cruza a la izquierda en el cruce de Fresa Mágica y avanza 150m.", 0.2f..0.6f),
        NavigationStep("Gira a la derecha por el Paseo del Limón Helado y avanza 100m.", 0.6f..0.9f),
        NavigationStep("¡Listo! Disfruta de tu helado en '${order.neveriaNombre}'.", 0.9f..1.0f)
    )

    // Trigger simulation timer when active
    LaunchedEffect(isSimulating) {
        if (isSimulating) {
            while (simulationProgress < 1.0f) {
                delay(800) // update every 800ms
                simulationProgress += 0.08f
                if (simulationProgress > 1.0f) simulationProgress = 1.0f
                
                // Detonate proximity alert on Wear OS when user gets closer than 100 meters
                val currentDist = (maxDistance * (1.0f - simulationProgress)).toInt()
                if (currentDist in 50..100) {
                    onSimulateProximity(order.neveriaId)
                }
            }
            isSimulating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Status Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.5.dp, MobileThemeColors.IceCreamPeach),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Camino a ${order.neveriaNombre}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MobileThemeColors.CocoaDarkText
                    )
                    Text(
                        text = "Distancia: $currentDistance metros",
                        fontSize = 14.sp,
                        color = MobileThemeColors.PeachText,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                
                Button(
                    onClick = {
                        if (simulationProgress >= 1.0f) {
                            simulationProgress = 0.0f
                        }
                        isSimulating = !isSimulating
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isSimulating) Color.Red else MobileThemeColors.MintText
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (isSimulating) "Pausar" else if (simulationProgress >= 1.0f) "Reiniciar" else "Iniciar Recorrido",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Map Canvas Box
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(Color.White, RoundedCornerShape(20.dp))
                .border(BorderStroke(2.dp, MobileThemeColors.IceCreamPeach), RoundedCornerShape(20.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                val w = size.width
                val h = size.height

                // Draw Grid/Streets in soft cream lines
                val gridColor = Color(0xFFF3EFE9)
                for (x in 0..w.toInt() step (w / 6).toInt()) {
                    drawLine(gridColor, Offset(x.toFloat(), 0f), Offset(x.toFloat(), h), 3f)
                }
                for (y in 0..h.toInt() step (h / 5).toInt()) {
                    drawLine(gridColor, Offset(0f, y.toFloat()), Offset(w, y.toFloat()), 3f)
                }

                // Path route coordinates
                val start = Offset(w * 0.15f, h * 0.8f)
                val mid1 = Offset(w * 0.45f, h * 0.8f)
                val mid2 = Offset(w * 0.45f, h * 0.35f)
                val end = Offset(w * 0.8f, h * 0.35f)

                // Draw static complete route path (muted peach)
                val routePath = Path().apply {
                    moveTo(start.x, start.y)
                    lineTo(mid1.x, mid1.y)
                    lineTo(mid2.x, mid2.y)
                    lineTo(end.x, end.y)
                }
                drawPath(routePath, MobileThemeColors.IceCreamPeach, style = Stroke(width = 10f))

                // Draw User Position (Dotted path travelled)
                val userPos = when {
                    simulationProgress <= 0.3f -> {
                        val segProgress = simulationProgress / 0.3f
                        Offset(start.x + (mid1.x - start.x) * segProgress, start.y)
                    }
                    simulationProgress <= 0.7f -> {
                        val segProgress = (simulationProgress - 0.3f) / 0.4f
                        Offset(mid1.x, mid1.y + (mid2.y - mid1.y) * segProgress)
                    }
                    else -> {
                        val segProgress = (simulationProgress - 0.7f) / 0.3f
                        Offset(mid2.x + (end.x - mid2.x) * segProgress, mid2.y)
                    }
                }

                // Draw walked path in Strawberry Pink color
                val walkPath = Path().apply {
                    moveTo(start.x, start.y)
                    if (simulationProgress > 0.3f) {
                        lineTo(mid1.x, mid1.y)
                        if (simulationProgress > 0.7f) {
                            lineTo(mid2.x, mid2.y)
                            lineTo(userPos.x, userPos.y)
                        } else {
                            lineTo(userPos.x, userPos.y)
                        }
                    } else {
                        lineTo(userPos.x, userPos.y)
                    }
                }
                drawPath(walkPath, MobileThemeColors.IceCreamPink, style = Stroke(width = 10f))

                // DRAW DESTINATION: Mint Ice cream Cone Pin
                val conePathDest = Path().apply {
                    moveTo(end.x - 8f, end.y)
                    lineTo(end.x + 8f, end.y)
                    lineTo(end.x, end.y + 16f)
                    close()
                }
                drawPath(conePathDest, MobileThemeColors.CocoaLightText)
                drawCircle(MobileThemeColors.MintText, 12f, end)
                drawCircle(Color.White, 4f, end)

                // DRAW USER: Strawberry Ice cream Cone Pin
                val conePathUser = Path().apply {
                    moveTo(userPos.x - 8f, userPos.y)
                    lineTo(userPos.x + 8f, userPos.y)
                    lineTo(userPos.x, userPos.y + 16f)
                    close()
                }
                drawPath(conePathUser, MobileThemeColors.CocoaLightText)
                drawCircle(MobileThemeColors.PinkText, 12f, userPos)
                drawCircle(Color.White, 4f, userPos)
            }
        }

        Text("INDICACIONES PASO A PASO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MobileThemeColors.PinkText)

        // Step-by-step directions list highlighting the active instruction
        LazyColumn(
            modifier = Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(directions) { index, step ->
                val isActive = simulationProgress in step.progressRange
                val isDone = simulationProgress > step.progressRange.endInclusive

                val cardBg = when {
                    isActive -> MobileThemeColors.IceCreamPeach
                    isDone -> Color.White.copy(alpha = 0.5f)
                    else -> Color.White
                }
                
                val borderMod = if (isActive) {
                    Modifier.border(1.5.dp, MobileThemeColors.PeachText, RoundedCornerShape(10.dp))
                } else Modifier.border(1.dp, MobileThemeColors.IceCreamPeach.copy(alpha = 0.5f), RoundedCornerShape(10.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = cardBg),
                    shape = RoundedCornerShape(10.dp),
                    modifier = borderMod.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when {
                                isDone -> Icons.Default.CheckCircle
                                isActive -> Icons.Default.DirectionsWalk
                                else -> Icons.Default.RadioButtonUnchecked
                            },
                            contentDescription = "Estado",
                            tint = when {
                                isDone -> MobileThemeColors.MintText
                                isActive -> MobileThemeColors.PeachText
                                else -> MobileThemeColors.CocoaMuted
                            },
                            modifier = Modifier.size(20.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Text(
                            text = step.instruction,
                            fontSize = 13.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (isActive) MobileThemeColors.CocoaDarkText else MobileThemeColors.CocoaLightText,
                            lineHeight = 16.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * SCREEN 3: CONTROL PANEL / SIMULATOR ACTIONS
 */
@Composable
fun SimulatorControlScreen(
    order: MockOrder?,
    notifications: List<MockNotification>,
    onActionTriggered: (String, String?, String?) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "SIMULAR PEDIDOS (Base de datos SQLite)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MobileThemeColors.PinkText
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.5.dp, MobileThemeColors.IceCreamPeach), RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                if (order == null) {
                    Button(
                        onClick = {
                            val demoOrder = MockOrder(
                                id = UUID.randomUUID().toString().take(8),
                                neveriaId = "nev_los_abuelos",
                                neveriaNombre = "Los Abuelos",
                                estado = "NUEVO",
                                tiempoEstimadoMinutos = 20,
                                fechaHoraMillis = System.currentTimeMillis(),
                                total = 220.0,
                                productos = listOf(
                                    MockProductLine("Paleta de Mango con Chile", 3, 35.0),
                                    MockProductLine("Helado Premium Pistache", 1, 115.0)
                                )
                            )
                            WearSyncService.activeOrderState = demoOrder
                            onActionTriggered("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.PinkText),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Crear Nuevo Pedido Activo", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Text(
                        text = "Pedido activo: ${order.neveriaNombre} - Estado: ${order.estado}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MobileThemeColors.CocoaDarkText
                    )
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                WearSyncService.activeOrderState = order.copy(estado = "ACEPTADO")
                                onActionTriggered("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.AceptadoBg),
                            border = BorderStroke(1.dp, MobileThemeColors.AceptadoText),
                            enabled = order.estado == "NUEVO",
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Aceptar", color = MobileThemeColors.AceptadoText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                WearSyncService.activeOrderState = order.copy(estado = "POSPUESTO")
                                onActionTriggered("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.PospuestoBg),
                            border = BorderStroke(1.dp, MobileThemeColors.PospuestoText),
                            enabled = order.estado == "NUEVO",
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Posponer", color = MobileThemeColors.PospuestoText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Button(
                            onClick = {
                                WearSyncService.activeOrderState = order.copy(estado = "ENTREGADO")
                                onActionTriggered("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.EntregadoBg),
                            border = BorderStroke(1.dp, MobileThemeColors.EntregadoText),
                            enabled = order.estado == "ACEPTADO",
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Entregar", color = MobileThemeColors.EntregadoText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = {
                                WearSyncService.activeOrderState = order.copy(estado = "RECHAZADO")
                                onActionTriggered("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.RechazadoBg),
                            border = BorderStroke(1.dp, MobileThemeColors.RechazadoText),
                            enabled = order.estado == "POSPUESTO",
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Rechazar", color = MobileThemeColors.RechazadoText, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    
                    OutlinedButton(
                        onClick = {
                            WearSyncService.activeOrderState = null
                            onActionTriggered("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Eliminar/Limpiar Pedido Activo")
                    }
                }
            }
        }

        // Section: Proximity simulation
        item {
            Text(
                text = "SIMULAR GEOCERCA (Alerta Proximidad)",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MobileThemeColors.PinkText
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.5.dp, MobileThemeColors.IceCreamPink), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Fuerza el envío inmediato de un mensaje de geolocalización de proximidad hacia tu reloj.",
                    fontSize = 12.sp,
                    color = MobileThemeColors.CocoaLightText,
                    lineHeight = 16.sp
                )
                
                Button(
                    onClick = {
                        onActionTriggered("mx.utng.snowtrail.ACTION_TRIGGER_PROXIMITY", "shop_id", "nev_los_abuelos")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.GoldBorder),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Detonar Alerta Proximidad (Los Abuelos)", color = MobileThemeColors.CocoaDarkText, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Section: Notification simulation
        item {
            Text(
                text = "SIMULAR PROMOCIONES Y ALERTAS",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = MobileThemeColors.PinkText
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White, RoundedCornerShape(16.dp))
                    .border(BorderStroke(1.5.dp, MobileThemeColors.IceCreamMint), RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val newPromo = MockNotification(
                            id = UUID.randomUUID().toString().take(6),
                            mensaje = "¡Promo Loca! 2x1 en helado de Chocolate Belga en Los Abuelos",
                            tipo = "PROMOCION",
                            leida = false,
                            fechaEnvio = System.currentTimeMillis()
                        )
                        WearSyncService.mockNotifications.add(0, newPromo)
                        onActionTriggered("mx.utng.snowtrail.ACTION_SYNC_NOTIFICATIONS", null, null)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.IceCreamMint),
                    border = BorderStroke(1.5.dp, MobileThemeColors.MintText),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Añadir Notificación de Promoción", color = MobileThemeColors.MintText, fontWeight = FontWeight.Bold)
                }
                
                Text(
                    text = "Bandeja actual (${notifications.size} alertas):",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MobileThemeColors.CocoaLightText
                )
                
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    notifications.take(3).forEach { notif ->
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "• " + notif.mensaje,
                                fontSize = 12.sp,
                                color = if (notif.leida) MobileThemeColors.CocoaMuted else MobileThemeColors.CocoaDarkText,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = if (notif.leida) "Leída" else "Nueva",
                                fontSize = 10.sp,
                                color = if (notif.leida) Color.Gray else MobileThemeColors.MintText,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
            }
        }
        
    }
}

/**
 * SCREEN: UBICACIÓN Y GPS CONTROL
 */
@Composable
fun GPSScreen(
    useRealGps: Boolean,
    userLat: Double,
    userLng: Double,
    onToggleRealGps: (Boolean) -> Unit,
    onGPSMoved: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "GPS Y DISTANCIAS CERCANAS (Rango 3 km)",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = MobileThemeColors.PinkText
        )
        
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White, RoundedCornerShape(16.dp))
                .border(BorderStroke(1.5.dp, MobileThemeColors.IceCreamLavender), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Usar Ubicación GPS Real",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MobileThemeColors.CocoaDarkText
                    )
                    Text(
                        text = "Si caminas en la vida real, las distancias en el reloj y celular se actualizarán automáticamente.",
                        fontSize = 10.sp,
                        color = MobileThemeColors.CocoaLightText,
                        lineHeight = 13.sp
                    )
                }
                Switch(
                    checked = useRealGps,
                    onCheckedChange = { onToggleRealGps(it) },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = MobileThemeColors.LavenderText,
                        checkedTrackColor = MobileThemeColors.IceCreamLavender
                    )
                )
            }
            
            Divider(color = MobileThemeColors.IceCreamLavender.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
            
            if (useRealGps) {
                Text(
                    text = "Coordenadas GPS reales:\nLat ${String.format(Locale.US, "%.5f", userLat)}\nLng ${String.format(Locale.US, "%.5f", userLng)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MobileThemeColors.LavenderText,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
                )
            } else {
                var sliderValue by remember { mutableFloatStateOf(0.0f) }
                
                Text(
                    text = "Simulación: Desplazamiento de ${sliderValue.toInt()} metros",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MobileThemeColors.CocoaDarkText
                )
                
                Text(
                    text = "Desplaza la barra para simular movimiento manual. Las distancias se actualizarán en el reloj y celular, ocultando las neverías a más de 3 km.",
                    fontSize = 11.sp,
                    color = MobileThemeColors.CocoaLightText,
                    lineHeight = 15.sp
                )
                
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    onValueChangeFinished = {
                        onGPSMoved(sliderValue)
                    },
                    valueRange = 0.0f..5000.0f,
                    colors = SliderDefaults.colors(
                        thumbColor = MobileThemeColors.LavenderText,
                        activeTrackColor = MobileThemeColors.IceCreamLavender
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Data helper models for mobile UI
data class CatalogItem(
    val nombre: String,
    val tipo: String,
    val nota: String,
    val precio: Double
)

data class CartLine(
    val item: CatalogItem,
    val qty: Int
)

data class NavigationStep(
    val instruction: String,
    val progressRange: ClosedFloatingPointRange<Float>
)
