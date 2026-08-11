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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.draw.alpha
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.viewinterop.AndroidView
import mx.utng.snowtrail.database.SnowTrailRepository
import mx.utng.snowtrail.service.MockNotification
import mx.utng.snowtrail.service.MockOrder
import mx.utng.snowtrail.service.MockProductLine
import mx.utng.snowtrail.service.MockShop
import mx.utng.snowtrail.service.MockPromotion
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
    var tvIpAddress by mutableStateOf("10.0.2.2")

    fun sendToTv(message: String) {
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            val ips = listOf(tvIpAddress, "10.0.2.2", "192.168.1.100", "127.0.0.1")
            for (ip in ips) {
                if (ip.isBlank()) continue
                try {
                    val client = java.net.Socket()
                    client.connect(java.net.InetSocketAddress(ip, 9090), 800)
                    val writer = java.io.PrintWriter(client.getOutputStream(), true)
                    writer.println(message)
                    writer.close()
                    client.close()
                } catch (e: Exception) {
                    // Try next fallback
                }
            }
        }
    }
    private var locationManager: LocationManager? = null
    private var locationListener: LocationListener? = null
    private var onLocationChangedCallback: ((Double, Double) -> Unit)? = null

    @android.annotation.SuppressLint("MissingPermission")
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

    @android.annotation.SuppressLint("MissingPermission")
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
        
        try {
            repository = SnowTrailRepository(applicationContext)
            repository.initializeDemoDataIfEmpty()
        } catch (e: Exception) {
            Toast.makeText(this, "Error Init DB: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }

        // Start Wear Sync Service to initialize DataLayer
        try {
            val startIntent = Intent(this, WearSyncService::class.java).apply {
                action = "mx.utng.snowtrail.ACTION_SYNC_ALL"
            }
            startService(startIntent)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        setContent {
            // Session states
            var loggedInUserEmail by remember { mutableStateOf<String?>(null) }
            var loggedInUserRole by remember { mutableStateOf<String?>(null) }

            // Navigation states: "home", "shop_detail", "route_navigation"
            var currentScreen by remember { mutableStateOf("home") }
            var selectedShopId by remember { mutableStateOf("") }
            var selectedTab by remember { mutableIntStateOf(0) }
            var shopToEdit by remember { mutableStateOf<MockShop?>(null) }
            var promoToEdit by remember { mutableStateOf<MockPromotion?>(null) }
            
            // Observe states dynamically from SQLite database
            var activeOrder by remember { mutableStateOf<MockOrder?>(null) }
            var shopsList by remember { mutableStateOf<List<MockShop>>(emptyList()) }
            var notificationsList by remember { mutableStateOf<List<MockNotification>>(emptyList()) }
            var promotionsList by remember { mutableStateOf<List<MockPromotion>>(emptyList()) }

            // Helper to reload from database
            val reloadFromDb = {
                try {
                    activeOrder = repository.getActiveOrder()
                    shopsList = repository.getShopsForUser(loggedInUserEmail)
                    notificationsList = repository.getNotifications()
                    promotionsList = repository.getPromotions()
                } catch (e: Exception) {
                    Toast.makeText(applicationContext, "Error reloadFromDb: ${e.message}", Toast.LENGTH_LONG).show()
                    e.printStackTrace()
                }
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
                             "nev_flor" to Pair(21.2100, -100.8800),
                             "nev_helarte" to Pair(21.1575, -100.9320)
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
            BackHandler(enabled = loggedInUserEmail != null && currentScreen != "home") {
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
                if (loggedInUserEmail == null) {
                    LoginRegisterScreen(
                        repository = repository,
                        onLoginSuccess = { email, role ->
                            loggedInUserEmail = email
                            loggedInUserRole = role
                            selectedTab = 0
                            currentScreen = "home"
                            reloadFromDb()
                        }
                    )
                } else if (loggedInUserRole == "ADMIN") {
                    AdminLayout(
                        repository = repository,
                        shopsList = shopsList,
                        promotionsList = promotionsList,
                        activeOrder = activeOrder,
                        notificationsList = notificationsList,
                        useRealGps = useRealGps,
                        userLatitude = userLatitude,
                        userLongitude = userLongitude,
                        tvIpAddress = tvIpAddress,
                        onTvIpAddressChange = { tvIpAddress = it },
                        onSendToTv = { sendToTv(it) },
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
                                    "nev_flor" to 8000.0,
                                    "nev_helarte" to 150.0
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
                                "nev_flor" to 8000.0,
                                "nev_helarte" to 150.0
                            )
                            simulatedBasePositions.forEach { (id, basePos) ->
                                val newDistance = kotlin.math.abs(sliderValue.toDouble() - basePos)
                                repository.updateShopDistance(id, newDistance)
                            }
                            triggerSync("mx.utng.snowtrail.ACTION_SYNC_SHOPS", null, null)
                        },
                        triggerSync = triggerSync,
                        reloadFromDb = reloadFromDb,
                        onLogout = {
                            loggedInUserEmail = null
                            loggedInUserRole = null
                            selectedTab = 0
                            currentScreen = "home"
                        }
                    )
                } else {
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
                                
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
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
                                                    .size(6.dp)
                                                    .background(MobileThemeColors.MintText, RoundedCornerShape(50))
                                            )
                                            Text(
                                                text = loggedInUserRole ?: "CLIENTE",
                                                color = MobileThemeColors.MintText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    // Logout button
                                    IconButton(
                                        onClick = {
                                            loggedInUserEmail = null
                                            loggedInUserRole = null
                                            selectedTab = 0
                                            currentScreen = "home"
                                        },
                                        modifier = Modifier
                                            .background(Color.White.copy(alpha = 0.6f), RoundedCornerShape(50))
                                            .size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Logout,
                                            contentDescription = "Cerrar sesión",
                                            tint = MobileThemeColors.PinkText,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Main screen content based on navigation
                        Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                            when (currentScreen) {
                                "home" -> Column(modifier = Modifier.fillMaxSize()) {
                                    val tabsList = if (loggedInUserRole == "ADMIN") {
                                        listOf(
                                            Triple("Explorar", Icons.Default.Explore, 0),
                                            Triple("Mi Pedido", Icons.Default.ReceiptLong, 1),
                                            Triple("Simular", Icons.Default.Settings, 2),
                                            Triple("GPS", Icons.Default.Place, 3)
                                        )
                                    } else {
                                        listOf(
                                            Triple("Explorar", Icons.Default.Explore, 0),
                                            Triple("Mi Pedido", Icons.Default.ReceiptLong, 1),
                                            Triple("GPS", Icons.Default.Place, 2),
                                            Triple("Mapa API", Icons.Default.Map, 3)
                                        )
                                    }

                                    // Rounded Floating Tab Navigation Bar
                                    TabRow(
                                        selectedTabIndex = selectedTab,
                                        containerColor = Color.Transparent,
                                        contentColor = MobileThemeColors.PinkText,
                                        indicator = { tabPositions ->
                                            if (selectedTab in tabPositions.indices) {
                                                TabRowDefaults.SecondaryIndicator(
                                                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                                                    color = MobileThemeColors.PinkText
                                                )
                                            }
                                        },
                                        modifier = Modifier
                                            .padding(horizontal = 16.dp, vertical = 8.dp)
                                            .background(Color.White, RoundedCornerShape(16.dp))
                                            .border(BorderStroke(1.dp, MobileThemeColors.IceCreamPeach.copy(alpha = 0.5f)), RoundedCornerShape(16.dp))
                                            .clip(RoundedCornerShape(16.dp))
                                    ) {
                                        tabsList.forEachIndexed { index, tabInfo ->
                                            Tab(
                                                selected = selectedTab == index,
                                                onClick = { 
                                                    selectedTab = index 
                                                    reloadFromDb()
                                                },
                                                text = { Text(tabInfo.first, fontWeight = FontWeight.Bold) },
                                                icon = { Icon(tabInfo.second, contentDescription = tabInfo.first) }
                                            )
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                                        if (loggedInUserRole == "ADMIN") {
                                            when (selectedTab) {
                                                0 -> ExploreShopsScreen(
                                                    shops = shopsList,
                                                    onToggleFavorite = { shopId ->
                                                        repository.toggleFavoriteShopForUser(loggedInUserEmail ?: "Admin@gmail.com", shopId)
                                                        reloadFromDb()
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
                                                                "nev_flor" to 8000.0,
                                                                "nev_helarte" to 150.0
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
                                                            "nev_flor" to 8000.0,
                                                            "nev_helarte" to 150.0
                                                        )
                                                        simulatedBasePositions.forEach { (id, basePos) ->
                                                            val newDistance = kotlin.math.abs(sliderValue.toDouble() - basePos)
                                                            repository.updateShopDistance(id, newDistance)
                                                        }
                                                        triggerSync("mx.utng.snowtrail.ACTION_SYNC_SHOPS", null, null)
                                                    }
                                                )
                                            }
                                        } else { // CLIENTE
                                            when (selectedTab) {
                                                0 -> ExploreShopsScreen(
                                                    shops = shopsList,
                                                    onToggleFavorite = { shopId ->
                                                        repository.toggleFavoriteShopForUser(loggedInUserEmail ?: "Cliente@gmail.com", shopId)
                                                        reloadFromDb()
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
                                                2 -> GPSScreen(
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
                                                3 -> PositionstackMapScreen(shops = shopsList)
                                            }
                                        }
                                    }
                                }
                                "shop_detail" -> ShopDetailScreen(
                                    shopId = selectedShopId,
                                    shops = shopsList,
                                    repository = repository,
                                    userEmail = loggedInUserEmail ?: "Cliente@gmail.com",
                                    onOrderCreated = { order ->
                                        triggerSync("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                                        val itemsSummary = order.productos.joinToString(", ") { "${it.cantidad}x ${it.nombre}" }
                                        sendToTv("ADD_ORDER:${order.neveriaId}|${order.id}|Cliente Móvil|Para recoger: 15 min|15 min|\$${order.total} MXN|$itemsSummary|NUEVO")
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
    var filterFavoritesOnly by remember { mutableStateOf(false) }
    
    // Only display shops within the 3 km (3000 meters) range
    val allInRange = shops.filter { it.distancia <= 3000.0 }
    val favsInRange = allInRange.filter { it.esFavorita }

    val filteredShops = allInRange.filter { shop ->
        val matchesSearch = shop.nombre.contains(searchQuery, ignoreCase = true)
        val matchesFav = if (filterFavoritesOnly) shop.esFavorita else true
        matchesSearch && matchesFav
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

        // Filter chips: Todas vs Favoritas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = !filterFavoritesOnly,
                onClick = { filterFavoritesOnly = false },
                label = { Text("🏪 Todas (${allInRange.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MobileThemeColors.IceCreamPink,
                    selectedLabelColor = MobileThemeColors.PinkText
                ),
                shape = RoundedCornerShape(12.dp)
            )

            FilterChip(
                selected = filterFavoritesOnly,
                onClick = { filterFavoritesOnly = true },
                label = { Text("⭐ Favoritas (${favsInRange.size})", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MobileThemeColors.GoldPastel,
                    selectedLabelColor = MobileThemeColors.GoldText
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        // Helpful banner if client has 0 favorites yet and is on "Todas"
        if (!filterFavoritesOnly && favsInRange.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = MobileThemeColors.GoldPastel.copy(alpha = 0.45f)),
                border = BorderStroke(1.dp, MobileThemeColors.GoldBorder),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("💡", fontSize = 20.sp)
                    Column {
                        Text(
                            text = "¡Elige tus heladerías favoritas!",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MobileThemeColors.GoldText
                        )
                        Text(
                            text = "Toca la estrella ⭐ en cualquiera de las neverías para guardarla en tu lista personalizada.",
                            fontSize = 11.sp,
                            color = MobileThemeColors.CocoaDarkText
                        )
                    }
                }
            }
        }

        // Empty state when filtering by Favorites and user has none
        if (filterFavoritesOnly && filteredShops.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.5.dp, MobileThemeColors.IceCreamPink),
                shape = RoundedCornerShape(20.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp).fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🍦 ⭐", fontSize = 34.sp)
                    Text(
                        text = "¡Elige tus heladerías favoritas!",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MobileThemeColors.PinkText,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Aún no has agregado ninguna heladería a tus favoritos.\n\nExplora la lista completa y toca la estrella ⭐ en tus heladerías preferidas para tenerlas siempre a la mano.",
                        fontSize = 12.sp,
                        color = MobileThemeColors.CocoaLightText,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = { filterFavoritesOnly = false },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.PinkText),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Ver todas las neverías", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }
            }
        } else {
            Text(
                text = if (filterFavoritesOnly) "Tus Neverías Favoritas (${filteredShops.size})" else "Neverías Cerca de Ti (${filteredShops.size} encontradas)",
                fontSize = 13.sp,
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
    userEmail: String,
    onOrderCreated: (MockOrder) -> Unit
) {
    val shop = shops.find { it.id == shopId } ?: return
    
    // Products catalog matching the selected shop
    val catalog = if (shop.id == "nev_helarte" || shop.nombre.contains("HELARTE", ignoreCase = true)) {
        listOf(
            CatalogItem("Copa Helarte Suprema (3 Bolas)", "Helado", "Vainilla, Fresa y Menta con fudge y chispas", 65.0),
            CatalogItem("Cono Tradición Artesanal", "Helado", "Doble bola en barquillo crujiente con cobertura", 48.0),
            CatalogItem("Sundae Especial de Chocolate", "Helado", "Bañado en salsa de chocolate y nueces selectas", 55.0),
            CatalogItem("Nieve de Fresa Silvestre", "Nieve", "100% fruta natural artesanal estilo Dolores", 38.0),
            CatalogItem("Paleta Rellena de Crema Helarte", "Paleta", "Rellena de chocolate y centro cremoso", 32.0),
            CatalogItem("Litro Familiar Helarte", "Helado", "Combina hasta 3 sabores a tu gusto", 140.0)
        )
    } else {
        listOf(
            CatalogItem("Nieve de Guanábana Especial", "Nieve", "Sabor clásico refrescante", 45.0),
            CatalogItem("Helado de Chocolate Belga", "Helado", "Sabor cremoso e intenso", 60.0),
            CatalogItem("Paleta de Fresas con Crema", "Paleta", "Con trozos naturales de fruta", 35.0),
            CatalogItem("Nieve de Limón con Chía", "Nieve", "Deliciosa y muy refrescante", 40.0),
            CatalogItem("Helado de Pistache Premium", "Helado", "Cremoso con trocitos tostados", 65.0)
        )
    }

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
                                productos = cart.map { MockProductLine(it.item.nombre, it.qty, it.item.precio) },
                                userEmail = userEmail
                            )
                            // Save order in SQLite DB directly
                            repository.saveOrder(newOrder)
                            onOrderCreated(newOrder)
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
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
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

        // Section: Proximity simulation
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

        // Section: Notification simulation
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

@Composable
fun LoginRegisterScreen(
    repository: SnowTrailRepository,
    onLoginSuccess: (String, String) -> Unit
) {
    var isRegisterMode by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var successMessage by remember { mutableStateOf("") }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MobileThemeColors.IceCreamPink.copy(alpha = 0.6f),
                        MobileThemeColors.OffWhiteVanilla
                    )
                )
            )
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MobileThemeColors.PureWhiteCard),
            shape = RoundedCornerShape(28.dp),
            border = BorderStroke(1.5.dp, MobileThemeColors.IceCreamPink),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier.fillMaxWidth().wrapContentHeight()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .background(MobileThemeColors.IceCreamPink, RoundedCornerShape(50)),
                    contentAlignment = Alignment.Center
                ) {
                    Text("🍦", fontSize = 32.sp)
                }
                
                Text(
                    text = if (isRegisterMode) "Registro de Cliente" else "Iniciar Sesión",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MobileThemeColors.CocoaDarkText
                )
                
                Text(
                    text = if (isRegisterMode) "Crea tu cuenta para pedir helados" else "Ingresa tus credenciales para continuar",
                    fontSize = 12.sp,
                    color = MobileThemeColors.CocoaLightText,
                    textAlign = TextAlign.Center
                )
                
                // Fields
                OutlinedTextField(
                    value = email,
                    onValueChange = { 
                        email = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    label = { Text("Correo Electrónico") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email", tint = MobileThemeColors.PinkText) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MobileThemeColors.IceCreamPink,
                        unfocusedBorderColor = MobileThemeColors.CocoaMuted.copy(alpha = 0.4f),
                        focusedTextColor = MobileThemeColors.CocoaDarkText,
                        unfocusedTextColor = MobileThemeColors.CocoaDarkText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                var passwordVisible by remember { mutableStateOf(false) }
                OutlinedTextField(
                    value = password,
                    onValueChange = { 
                        password = it
                        errorMessage = ""
                        successMessage = ""
                    },
                    label = { Text("Contraseña") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password", tint = MobileThemeColors.PinkText) },
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = if (passwordVisible) "Ocultar" else "Mostrar",
                                tint = MobileThemeColors.CocoaMuted
                            )
                        }
                    },
                    singleLine = true,
                    visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MobileThemeColors.IceCreamPink,
                        unfocusedBorderColor = MobileThemeColors.CocoaMuted.copy(alpha = 0.4f),
                        focusedTextColor = MobileThemeColors.CocoaDarkText,
                        unfocusedTextColor = MobileThemeColors.CocoaDarkText
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (isRegisterMode) {
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { 
                            confirmPassword = it
                            errorMessage = ""
                            successMessage = ""
                        },
                        label = { Text("Confirmar Contraseña") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Confirm Password", tint = MobileThemeColors.PinkText) },
                        singleLine = true,
                        visualTransformation = if (passwordVisible) androidx.compose.ui.text.input.VisualTransformation.None else androidx.compose.ui.text.input.PasswordVisualTransformation(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MobileThemeColors.IceCreamPink,
                            unfocusedBorderColor = MobileThemeColors.CocoaMuted.copy(alpha = 0.4f),
                            focusedTextColor = MobileThemeColors.CocoaDarkText,
                            unfocusedTextColor = MobileThemeColors.CocoaDarkText
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                
                if (successMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9)),
                        border = BorderStroke(1.5.dp, Color(0xFF4CAF50)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("✨", fontSize = 18.sp)
                            Text(
                                text = successMessage,
                                color = Color(0xFF2E7D32),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
                
                if (errorMessage.isNotEmpty()) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                        border = BorderStroke(1.5.dp, Color(0xFFEF5350)),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("⚠️", fontSize = 16.sp)
                            Text(
                                text = errorMessage,
                                color = Color(0xFFC62828),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                }
                
                // Submit Button
                Button(
                    onClick = {
                        if (email.isBlank() || password.isBlank()) {
                            errorMessage = "Por favor completa todos los campos"
                            successMessage = ""
                            return@Button
                        }
                        
                        if (isRegisterMode) {
                            if (password != confirmPassword) {
                                errorMessage = "Las contraseñas no coinciden"
                                successMessage = ""
                                return@Button
                            }
                            if (password.length < 6) {
                                errorMessage = "La contraseña debe tener al menos 6 caracteres"
                                successMessage = ""
                                return@Button
                            }
                            
                            // Check if they try to register Admin@gmail.com
                            if (email.trim().equals("Admin@gmail.com", ignoreCase = true)) {
                                errorMessage = "No puedes registrarte con esta cuenta"
                                successMessage = ""
                                return@Button
                            }

                            val success = repository.registerUser(email.trim(), password, "CLIENTE")
                            if (success) {
                                isRegisterMode = false
                                successMessage = "¡Usuario registrado correctamente! Inicia sesión para continuar"
                                errorMessage = ""
                                password = ""
                                confirmPassword = ""
                            } else {
                                errorMessage = "El correo ya está registrado"
                                successMessage = ""
                            }
                        } else {
                            val role = repository.authenticateUser(email.trim(), password)
                            if (role != null) {
                                onLoginSuccess(email.trim(), role)
                            } else {
                                errorMessage = "Credenciales incorrectas"
                                successMessage = ""
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.PinkText),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (isRegisterMode) "Registrarse" else "Ingresar",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                
                // Switch mode
                Text(
                    text = if (isRegisterMode) "¿Ya tienes cuenta? Inicia sesión" else "¿No tienes cuenta? Regístrate aquí",
                    color = MobileThemeColors.LavenderText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            isRegisterMode = !isRegisterMode
                            errorMessage = ""
                            successMessage = ""
                            email = ""
                            password = ""
                            confirmPassword = ""
                        }
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
fun AdminLayout(
    repository: SnowTrailRepository,
    shopsList: List<MockShop>,
    promotionsList: List<MockPromotion>,
    activeOrder: MockOrder?,
    notificationsList: List<MockNotification>,
    useRealGps: Boolean,
    userLatitude: Double,
    userLongitude: Double,
    tvIpAddress: String,
    onTvIpAddressChange: (String) -> Unit,
    onSendToTv: (String) -> Unit,
    onToggleRealGps: (Boolean) -> Unit,
    onGPSMoved: (Float) -> Unit,
    triggerSync: (String, String?, String?) -> Unit,
    reloadFromDb: () -> Unit,
    onLogout: () -> Unit
) {
    var adminTab by remember { mutableIntStateOf(0) }
    var adminSubScreen by remember { mutableStateOf("dashboard") }
    var selectedShopToEdit by remember { mutableStateOf<MockShop?>(null) }
    var selectedPromoToEdit by remember { mutableStateOf<MockPromotion?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE2F9EE))
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val strawberryOffsets = listOf(
                Pair(0.1f, 0.15f), Pair(0.85f, 0.08f), Pair(0.05f, 0.45f),
                Pair(0.9f, 0.35f), Pair(0.15f, 0.72f), Pair(0.88f, 0.65f),
                Pair(0.5f, 0.2f), Pair(0.55f, 0.8f), Pair(0.08f, 0.9f),
                Pair(0.92f, 0.88f), Pair(0.7f, 0.5f), Pair(0.3f, 0.55f)
            )
            strawberryOffsets.forEach { (x, y) ->
                Text(
                    text = "🍓",
                    fontSize = 24.sp,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset(
                            x = (x * 320).dp,
                            y = (y * 600).dp
                        )
                        .alpha(0.18f)
                )
            }
        }

        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 12.dp, start = 20.dp, end = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(Color.White, CircleShape)
                                .border(BorderStroke(1.5.dp, Color(0xFFEF9A9A)), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("🍦", fontSize = 20.sp)
                        }

                        OutlinedTextField(
                            value = tvIpAddress,
                            onValueChange = onTvIpAddressChange,
                            label = { Text("IP de la TV", fontSize = 9.sp) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MobileThemeColors.IceCreamPink,
                                focusedTextColor = MobileThemeColors.CocoaDarkText,
                                unfocusedTextColor = MobileThemeColors.CocoaDarkText
                            ),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp),
                            modifier = Modifier.width(120.dp).height(48.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(20.dp))
                            .border(BorderStroke(1.5.dp, Color(0xFFEF9A9A)), RoundedCornerShape(20.dp))
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "SnowTrail Admin",
                            color = MobileThemeColors.PinkText,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                if (adminSubScreen == "edit_shop" && selectedShopToEdit != null) {
                    AdminEditShopScreen(
                        shop = selectedShopToEdit!!,
                        onSave = { updatedShop ->
                            repository.saveShop(updatedShop)
                            reloadFromDb()
                            triggerSync("mx.utng.snowtrail.ACTION_SYNC_SHOPS", null, null)
                            adminSubScreen = "dashboard"
                        },
                        onCancel = { adminSubScreen = "dashboard" }
                    )
                } else if (adminSubScreen == "edit_promo" && selectedPromoToEdit != null) {
                    AdminEditPromoScreen(
                        promo = selectedPromoToEdit!!,
                        onSave = { updatedPromo ->
                            repository.savePromotion(updatedPromo)
                            reloadFromDb()
                            triggerSync("mx.utng.snowtrail.ACTION_SYNC_NOTIFICATIONS", null, null)
                            adminSubScreen = "dashboard"
                        },
                        onCancel = { adminSubScreen = "dashboard" }
                    )
                } else {
                    when (adminTab) {
                        0 -> AdminDashboardTab(
                            shopsList = shopsList,
                            promotionsList = promotionsList,
                            onSelectShopForTv = { shopId ->
                                onSendToTv("SELECT_SHOP:$shopId")
                            },
                            onEditShop = {
                                selectedShopToEdit = it
                                adminSubScreen = "edit_shop"
                            },
                            onDeleteShop = { shopId ->
                                repository.deleteShop(shopId)
                                reloadFromDb()
                                triggerSync("mx.utng.snowtrail.ACTION_SYNC_SHOPS", null, null)
                            },
                            onEditPromo = {
                                selectedPromoToEdit = it
                                adminSubScreen = "edit_promo"
                            },
                            onDeletePromo = { promoId ->
                                repository.deletePromotion(promoId)
                                reloadFromDb()
                                triggerSync("mx.utng.snowtrail.ACTION_SYNC_NOTIFICATIONS", null, null)
                            }
                        )
                        1 -> AdminAddTab(
                            repository = repository,
                            shopsList = shopsList,
                            reloadFromDb = reloadFromDb,
                            triggerSync = triggerSync,
                            onSavePromotionToTv = { shopId, promoId, name, start, end, note ->
                                onSendToTv("ADD_PROMO:$shopId|$promoId|$name|$start|$end|$note|🍓")
                            },
                            onSendToTv = onSendToTv,
                            onSuccess = { adminTab = 0 }
                        )
                        2 -> AdminOrdersHistoryTab(
                            repository = repository,
                            reloadFromDb = reloadFromDb,
                            triggerSync = triggerSync
                        )
                        3 -> AdminProfileTab(
                            activeOrder = activeOrder,
                            notificationsList = notificationsList,
                            useRealGps = useRealGps,
                            userLatitude = userLatitude,
                            userLongitude = userLongitude,
                            onToggleRealGps = onToggleRealGps,
                            onGPSMoved = onGPSMoved,
                            onActionTriggered = { actionName, extraKey, extraVal ->
                                triggerSync(actionName, extraKey, extraVal)
                                reloadFromDb()
                            },
                            onLogout = onLogout
                        )
                        4 -> PositionstackMapScreen(shops = shopsList)
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .border(BorderStroke(1.dp, Color(0xFFEEEEEE)))
                    .padding(vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            adminTab = 0
                            adminSubScreen = "dashboard"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Home,
                            contentDescription = "Home",
                            tint = if (adminTab == 0 && adminSubScreen == "dashboard") Color(0xFFEF9A9A) else Color(0xFFA7B8C4),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            adminTab = 1
                            adminSubScreen = "dashboard"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Agregar",
                            tint = if (adminTab == 1 && adminSubScreen == "dashboard") Color(0xFFEF9A9A) else Color(0xFFA7B8C4),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            adminTab = 2
                            adminSubScreen = "dashboard"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Historial",
                            tint = if (adminTab == 2 && adminSubScreen == "dashboard") Color(0xFFEF9A9A) else Color(0xFFA7B8C4),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            adminTab = 3
                            adminSubScreen = "dashboard"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Perfil",
                            tint = if (adminTab == 3 && adminSubScreen == "dashboard") Color(0xFFEF9A9A) else Color(0xFFA7B8C4),
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            adminTab = 4
                            adminSubScreen = "dashboard"
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Map,
                            contentDescription = "Mapa API",
                            tint = if (adminTab == 4 && adminSubScreen == "dashboard") Color(0xFFEF9A9A) else Color(0xFFA7B8C4),
                            modifier = Modifier.size(30.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDashboardTab(
    shopsList: List<MockShop>,
    promotionsList: List<MockPromotion>,
    onSelectShopForTv: (String) -> Unit = {},
    onEditShop: (MockShop) -> Unit,
    onDeleteShop: (String) -> Unit,
    onEditPromo: (MockPromotion) -> Unit,
    onDeletePromo: (String) -> Unit
) {
    var selectedSection by remember { mutableStateOf("neverias") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selectedSection == "neverias") Color(0xFFEF9A9A) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedSection = "neverias" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Neverías",
                    fontWeight = FontWeight.Bold,
                    color = if (selectedSection == "neverias") Color.White else Color.Gray
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (selectedSection == "promociones") Color(0xFFEF9A9A) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { selectedSection = "promociones" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Promociones",
                    fontWeight = FontWeight.Bold,
                    color = if (selectedSection == "promociones") Color.White else Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            if (selectedSection == "neverias") {
                items(shopsList) { shop ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = shop.nombre,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF333333)
                                )
                                Text(
                                    text = "${shop.distancia.toInt()} m",
                                    fontSize = 12.sp,
                                    color = Color.Gray
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = "📍 ${shop.direccion}", fontSize = 13.sp, color = Color.DarkGray)
                            Text(text = "🕒 ${shop.horario}", fontSize = 13.sp, color = Color.DarkGray)
                            Text(text = "📞 ${shop.contacto}", fontSize = 13.sp, color = Color.DarkGray)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { onSelectShopForTv(shop.id) }) {
                                    Text("📺 Ver en TV", color = Color(0xFF4A34AC), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { onEditShop(shop) }) {
                                    Text("Editar", color = Color(0xFFEF9A9A), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { onDeleteShop(shop.id) }) {
                                    Text("Eliminar", color = Color.Red.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            } else {
                items(promotionsList) { promo ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = promo.nombre,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF333333)
                            )
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = "📅 Inicio: ${promo.fechaInicio} | Fin: ${promo.fechaFin}", fontSize = 13.sp, color = Color.DarkGray)
                            Text(text = "📝 Nota: ${promo.nota}", fontSize = 13.sp, color = Color.DarkGray)
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                TextButton(onClick = { onEditPromo(promo) }) {
                                    Text("Editar", color = Color(0xFFEF9A9A), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                TextButton(onClick = { onDeletePromo(promo.id) }) {
                                    Text("Eliminar", color = Color.Red.copy(alpha = 0.7f), fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminAddTab(
    repository: SnowTrailRepository,
    shopsList: List<MockShop> = emptyList(),
    reloadFromDb: () -> Unit,
    triggerSync: (String, String?, String?) -> Unit,
    onSavePromotionToTv: (String, String, String, String, String, String) -> Unit = { _, _, _, _, _, _ -> },
    onSendToTv: (String) -> Unit = {},
    onSuccess: () -> Unit
) {
    var formType by remember { mutableStateOf("neveria") }
    
    var shopName by remember { mutableStateOf("") }
    var shopHorario by remember { mutableStateOf("") }
    var shopContacto by remember { mutableStateOf("") }
    var shopDireccion by remember { mutableStateOf("") }
    
    var promoName by remember { mutableStateOf("") }
    var promoStart by remember { mutableStateOf("") }
    var promoEnd by remember { mutableStateOf("") }
    var promoNote by remember { mutableStateOf("") }

    var selectedShopIdForPromo by remember { mutableStateOf("nev_los_abuelos") }
    var selectedShopNameForPromo by remember { mutableStateOf("Los Abuelos") }
    var expandedShopSelect by remember { mutableStateOf(false) }

    var selectedShopIndex by remember { mutableIntStateOf(0) }
    var selectedProductIndex by remember { mutableIntStateOf(0) }
    var orderSentSuccessMessage by remember { mutableStateOf("") }
    val orderProducts = remember { mutableStateListOf<MockProductLine>() }
    var selectedQty by remember { mutableIntStateOf(1) }
    
    val quickProducts = listOf(
        Pair("Nieve de Guanábana Especial", 45.0),
        Pair("Helado de Chocolate Belga", 60.0),
        Pair("Paleta de Fresas con Crema", 35.0),
        Pair("Nieve de Limón con Chía", 40.0),
        Pair("Helado de Pistache Premium", 65.0)
    )

    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp)
                .background(Color.White, RoundedCornerShape(12.dp))
                .padding(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (formType == "neveria") Color(0xFFEF9A9A) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { formType = "neveria"; errorMessage = "" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nueva Nevería", fontWeight = FontWeight.Bold, color = if (formType == "neveria") Color.White else Color.Gray, fontSize = 11.sp, maxLines = 1)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (formType == "promocion") Color(0xFFEF9A9A) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { formType = "promocion"; errorMessage = "" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nueva Promoción", fontWeight = FontWeight.Bold, color = if (formType == "promocion") Color.White else Color.Gray, fontSize = 11.sp, maxLines = 1)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .background(
                        if (formType == "pedido") Color(0xFFEF9A9A) else Color.Transparent,
                        RoundedCornerShape(8.dp)
                    )
                    .clickable { formType = "pedido"; errorMessage = "" }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Nuevo Pedido", fontWeight = FontWeight.Bold, color = if (formType == "pedido") Color.White else Color.Gray, fontSize = 11.sp, maxLines = 1)
            }
        }

        Text(
            text = if (formType == "neveria") "Agregar Nevería" else if (formType == "promocion") "Agregar Promoción" else "Realizar Pedido Rápido",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (formType == "neveria") {
                    OutlinedTextField(
                        value = shopName,
                        onValueChange = { shopName = it },
                        label = { Text("Nombre") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = shopHorario,
                        onValueChange = { shopHorario = it },
                        label = { Text("Horario") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = shopContacto,
                        onValueChange = { shopContacto = it },
                        label = { Text("Contacto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = shopDireccion,
                        onValueChange = { shopDireccion = it },
                        label = { Text("Dirección") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else if (formType == "promocion") {
                    // Neveria selector
                    Text("Para Heladería:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { expandedShopSelect = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.IceCreamPink),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "📺 Seleccionada: $selectedShopNameForPromo", color = MobileThemeColors.CocoaDarkText, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = expandedShopSelect,
                            onDismissRequest = { expandedShopSelect = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            shopsList.forEach { shop ->
                                DropdownMenuItem(
                                    text = { Text(shop.nombre) },
                                    onClick = {
                                        selectedShopIdForPromo = shop.id
                                        selectedShopNameForPromo = shop.nombre
                                        expandedShopSelect = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = promoName,
                        onValueChange = { promoName = it },
                        label = { Text("Nombre de la Promoción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = promoStart,
                        onValueChange = { promoStart = it },
                        label = { Text("Fecha Inicio (AAAA-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = promoEnd,
                        onValueChange = { promoEnd = it },
                        label = { Text("Fecha Fin (AAAA-MM-DD)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = promoNote,
                        onValueChange = { promoNote = it },
                        label = { Text("Nota / Descripción") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else { // formType == "pedido"
                    Text("Seleccionar Nevería:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    var shopExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { shopExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.IceCreamPink),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "🏪: ${shopsList.getOrNull(selectedShopIndex)?.nombre ?: "Seleccionar"}", color = MobileThemeColors.CocoaDarkText, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = shopExpanded,
                            onDismissRequest = { shopExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            shopsList.forEachIndexed { index, shop ->
                                DropdownMenuItem(
                                    text = { Text(shop.nombre) },
                                    onClick = {
                                        selectedShopIndex = index
                                        shopExpanded = false
                                        orderProducts.clear()
                                        orderSentSuccessMessage = ""
                                    }
                                )
                            }
                        }
                    }

                    Text("Seleccionar Producto:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                    var productExpanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { productExpanded = true },
                            colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.IceCreamPink),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "🍦: ${quickProducts[selectedProductIndex].first} ($${quickProducts[selectedProductIndex].second})", color = MobileThemeColors.CocoaDarkText, fontWeight = FontWeight.Bold)
                        }
                        DropdownMenu(
                            expanded = productExpanded,
                            onDismissRequest = { productExpanded = false },
                            modifier = Modifier.fillMaxWidth(0.8f)
                        ) {
                            quickProducts.forEachIndexed { index, prod ->
                                DropdownMenuItem(
                                    text = { Text("${prod.first} ($${prod.second})") },
                                    onClick = {
                                        selectedProductIndex = index
                                        productExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                    ) {
                        Text("Cantidad:", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .border(BorderStroke(1.dp, Color.LightGray), RoundedCornerShape(8.dp))
                        ) {
                            IconButton(
                                onClick = { if (selectedQty > 1) selectedQty-- },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("-", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                            Text(
                                text = selectedQty.toString(),
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 12.dp),
                                color = Color.Black
                            )
                            IconButton(
                                onClick = { selectedQty++ },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Text("+", fontWeight = FontWeight.Bold, color = Color.DarkGray)
                            }
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        Button(
                            onClick = {
                                val prod = quickProducts[selectedProductIndex]
                                val existingIndex = orderProducts.indexOfFirst { it.nombre == prod.first }
                                if (existingIndex != -1) {
                                    val current = orderProducts[existingIndex]
                                    orderProducts[existingIndex] = current.copy(cantidad = current.cantidad + selectedQty)
                                } else {
                                    orderProducts.add(MockProductLine(prod.first, selectedQty, prod.second))
                                }
                                selectedQty = 1
                                orderSentSuccessMessage = ""
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.IceCreamPink),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Añadir", color = MobileThemeColors.CocoaDarkText, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }

                    if (orderProducts.isNotEmpty()) {
                        Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))
                        Text("Productos Añadidos:", fontWeight = FontWeight.Bold, color = Color(0xFF333333), fontSize = 13.sp)
                        
                        orderProducts.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFFFAFAFA), RoundedCornerShape(8.dp))
                                    .border(BorderStroke(0.5.dp, Color(0xFFEEEEEE)), RoundedCornerShape(8.dp))
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(item.nombre, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                                    Text("${item.cantidad}x $${item.precioUnitario} = $${item.cantidad * item.precioUnitario}", fontSize = 11.sp, color = Color.Gray)
                                }
                                IconButton(
                                    onClick = { orderProducts.remove(item) },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = Color.Red, modifier = Modifier.size(16.dp))
                                }
                            }
                        }

                        val totalAmount = orderProducts.sumOf { item -> item.cantidad * item.precioUnitario }
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Total del Pedido:", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF333333))
                            Text("$${totalAmount}", fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Color(0xFFB52D5E))
                        }
                    }

                    if (orderSentSuccessMessage.isNotEmpty()) {
                        Text(text = orderSentSuccessMessage, color = Color(0xFF388E3C), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (formType == "neveria") {
                                if (shopName.isBlank() || shopDireccion.isBlank()) {
                                    errorMessage = "Nombre y dirección son requeridos"
                                    return@Button
                                }
                                val newShopId = "nev_" + UUID.randomUUID().toString().take(6)
                                val newShop = MockShop(
                                    id = newShopId,
                                    nombre = shopName,
                                    distancia = 1500.0,
                                    esFavorita = false,
                                    tienePromocion = false,
                                    horario = shopHorario,
                                    contacto = shopContacto,
                                    direccion = shopDireccion
                                )
                                repository.saveShop(newShop)
                                reloadFromDb()
                                triggerSync("mx.utng.snowtrail.ACTION_SYNC_SHOPS", null, null)
                                onSuccess()
                            } else if (formType == "promocion") {
                                if (promoName.isBlank() || promoStart.isBlank() || promoEnd.isBlank()) {
                                    errorMessage = "Nombre y fechas de inicio/fin son requeridos"
                                    return@Button
                                }
                                val newPromoId = "promo_" + UUID.randomUUID().toString().take(6)
                                val newPromo = MockPromotion(
                                    id = newPromoId,
                                    nombre = promoName,
                                    fechaInicio = promoStart,
                                    fechaFin = promoEnd,
                                    nota = promoNote
                                )
                                repository.savePromotion(newPromo)
                                reloadFromDb()
                                triggerSync("mx.utng.snowtrail.ACTION_SYNC_NOTIFICATIONS", null, null)
                                onSavePromotionToTv(selectedShopIdForPromo, newPromoId, promoName, promoStart, promoEnd, promoNote)
                                onSuccess()
                            } else { // formType == "pedido"
                                val targetShop = shopsList.getOrNull(selectedShopIndex)
                                if (targetShop == null) {
                                    errorMessage = "Selecciona una nevería válida"
                                    return@Button
                                }
                                if (orderProducts.isEmpty()) {
                                    errorMessage = "Debes añadir al menos un producto al pedido"
                                    return@Button
                                }
                                val orderId = UUID.randomUUID().toString().take(8)
                                val totalAmount = orderProducts.sumOf { it.cantidad * it.precioUnitario }
                                val newOrder = MockOrder(
                                    id = orderId,
                                    neveriaId = targetShop.id,
                                    neveriaNombre = targetShop.nombre,
                                    estado = "NUEVO",
                                    tiempoEstimadoMinutos = 15,
                                    fechaHoraMillis = System.currentTimeMillis(),
                                    total = totalAmount,
                                    productos = orderProducts.toList(),
                                    userEmail = "Admin@gmail.com"
                                )
                                repository.saveOrder(newOrder)
                                
                                val itemsSummary = orderProducts.joinToString(", ") { "${it.cantidad}x ${it.nombre}" }
                                onSendToTv("ADD_ORDER:${targetShop.id}|$orderId|Administrador|Para recoger: 15 min|15 min|\$${totalAmount} MXN|$itemsSummary|NUEVO")
                                
                                orderSentSuccessMessage = "¡Pedido $orderId enviado con éxito a la TV!"
                                orderProducts.clear()
                                reloadFromDb()
                                onSuccess()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF9A9A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(if (formType == "pedido") "Pedir" else "Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            shopName = ""
                            shopHorario = ""
                            shopContacto = ""
                            shopDireccion = ""
                            promoName = ""
                            promoStart = ""
                            promoEnd = ""
                            promoNote = ""
                            errorMessage = ""
                            orderProducts.clear()
                            onSuccess()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA7B8C4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminEditShopScreen(
    shop: MockShop,
    onSave: (MockShop) -> Unit,
    onCancel: () -> Unit
) {
    var shopName by remember { mutableStateOf(shop.nombre) }
    var shopHorario by remember { mutableStateOf(shop.horario) }
    var shopContacto by remember { mutableStateOf(shop.contacto) }
    var shopDireccion by remember { mutableStateOf(shop.direccion) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Editar Nevería",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = shopName,
                    onValueChange = { shopName = it },
                    label = { Text("Nombre") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shopHorario,
                    onValueChange = { shopHorario = it },
                    label = { Text("Horario") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shopContacto,
                    onValueChange = { shopContacto = it },
                    label = { Text("Contacto") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = shopDireccion,
                    onValueChange = { shopDireccion = it },
                    label = { Text("Dirección") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (shopName.isBlank() || shopDireccion.isBlank()) {
                                errorMessage = "Nombre y dirección son requeridos"
                                return@Button
                            }
                            onSave(
                                shop.copy(
                                    nombre = shopName,
                                    horario = shopHorario,
                                    contacto = shopContacto,
                                    direccion = shopDireccion
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF9A9A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA7B8C4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminEditPromoScreen(
    promo: MockPromotion,
    onSave: (MockPromotion) -> Unit,
    onCancel: () -> Unit
) {
    var promoName by remember { mutableStateOf(promo.nombre) }
    var promoStart by remember { mutableStateOf(promo.fechaInicio) }
    var promoEnd by remember { mutableStateOf(promo.fechaFin) }
    var promoNote by remember { mutableStateOf(promo.nota) }
    var errorMessage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Editar Promoción",
            fontSize = 22.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF333333),
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = promoName,
                    onValueChange = { promoName = it },
                    label = { Text("Nombre de la Promoción") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = promoStart,
                    onValueChange = { promoStart = it },
                    label = { Text("Fecha Inicio (AAAA-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = promoEnd,
                    onValueChange = { promoEnd = it },
                    label = { Text("Fecha Fin (AAAA-MM-DD)") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = promoNote,
                    onValueChange = { promoNote = it },
                    label = { Text("Nota / Descripción") },
                    modifier = Modifier.fillMaxWidth()
                )

                if (errorMessage.isNotEmpty()) {
                    Text(text = errorMessage, color = Color.Red, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = {
                            if (promoName.isBlank() || promoStart.isBlank() || promoEnd.isBlank()) {
                                errorMessage = "Nombre y fechas de inicio/fin son requeridos"
                                return@Button
                            }
                            onSave(
                                promo.copy(
                                    nombre = promoName,
                                    fechaInicio = promoStart,
                                    fechaFin = promoEnd,
                                    nota = promoNote
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF9A9A)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Guardar", color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = onCancel,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFA7B8C4)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancelar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AdminProfileTab(
    activeOrder: MockOrder?,
    notificationsList: List<MockNotification>,
    useRealGps: Boolean,
    userLatitude: Double,
    userLongitude: Double,
    onToggleRealGps: (Boolean) -> Unit,
    onGPSMoved: (Float) -> Unit,
    onActionTriggered: (String, String?, String?) -> Unit,
    onLogout: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFFEF9A9A), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👑", fontSize = 30.sp)
                }

                Column {
                    Text(text = "Administrador", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    Text(text = "Admin@gmail.com", fontSize = 14.sp, color = Color.Gray)
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Button(
                        onClick = onLogout,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF9A9A)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text("Cerrar Sesión", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "📍 Simulador GPS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(8.dp))
                GPSScreen(
                    useRealGps = useRealGps,
                    userLat = userLatitude,
                    userLng = userLongitude,
                    onToggleRealGps = onToggleRealGps,
                    onGPSMoved = onGPSMoved
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "⚙️ Simulador de Pedidos", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                Spacer(modifier = Modifier.height(8.dp))
                SimulatorControlScreen(
                    order = activeOrder,
                    notifications = notificationsList,
                    onActionTriggered = onActionTriggered
                )
            }
        }
    }
}

@Composable
@android.annotation.SuppressLint("NewApi")
fun PositionstackMapScreen(shops: List<MockShop> = emptyList()) {
    var searchQuery by remember { mutableStateOf("") }
    var resolvedAddress by remember { mutableStateOf("Dolores Hidalgo, Gto, México") }
    var latitude by remember { mutableDoubleStateOf(21.1561) }
    var longitude by remember { mutableDoubleStateOf(-100.9312) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val coroutineScope = rememberCoroutineScope()
    val apiKey = "3819e173f0b09bac5de6773a1f641eea"

    val performReverseGeocode = { lat: Double, lng: Double ->
        coroutineScope.launch(Dispatchers.IO) {
            try {
                val url = java.net.URL("http://api.positionstack.com/v1/reverse?access_key=$apiKey&query=$lat,$lng")
                val connection = url.openConnection() as java.net.HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000

                val responseCode = connection.responseCode
                if (responseCode == 200) {
                    val stream = connection.inputStream
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
                    val sb = StringBuilder()
                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        sb.append(line)
                    }
                    reader.close()
                    stream.close()

                    val json = org.json.JSONObject(sb.toString())
                    val data = json.optJSONArray("data")
                    if (data != null && data.length() > 0) {
                        val first = data.getJSONObject(0)
                        val label = first.optString("label", "$lat, $lng")
                        withContext(Dispatchers.Main) {
                            resolvedAddress = label
                        }
                    }
                }
                connection.disconnect()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    val performForwardGeocode = { query: String ->
        if (query.isNotBlank()) {
            isLoading = true
            errorMessage = ""
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val restrictedQuery = if (query.lowercase().contains("dolores hidalgo")) {
                        query.trim()
                    } else {
                        "${query.trim()}, Dolores Hidalgo, Gto, México"
                    }
                    val encodedQuery = java.net.URLEncoder.encode(restrictedQuery, "UTF-8")
                    val url = java.net.URL("http://api.positionstack.com/v1/forward?access_key=$apiKey&query=$encodedQuery")
                    val connection = url.openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000

                    val responseCode = connection.responseCode
                    if (responseCode == 200) {
                        val stream = connection.inputStream
                        val reader = java.io.BufferedReader(java.io.InputStreamReader(stream))
                        val sb = StringBuilder()
                        var line: String?
                        while (reader.readLine().also { line = it } != null) {
                            sb.append(line)
                        }
                        reader.close()
                        stream.close()

                        val json = org.json.JSONObject(sb.toString())
                        val data = json.optJSONArray("data")
                        if (data != null && data.length() > 0) {
                            val first = data.getJSONObject(0)
                            val lat = first.optDouble("latitude", 21.1561)
                            val lng = first.optDouble("longitude", -100.9312)
                            val label = first.optString("label", query)
                            withContext(Dispatchers.Main) {
                                latitude = lat
                                longitude = lng
                                resolvedAddress = label
                                isLoading = false
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                errorMessage = "No se encontraron resultados en Positionstack"
                                isLoading = false
                            }
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            errorMessage = "Error de API: código $responseCode"
                            isLoading = false
                        }
                    }
                    connection.disconnect()
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        errorMessage = "Error de conexión: ${e.message}"
                        isLoading = false
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        performReverseGeocode(latitude, longitude)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MobileThemeColors.OffWhiteVanilla)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MobileThemeColors.PureWhiteCard),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, MobileThemeColors.IceCreamPink),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Buscador Geográfico (Positionstack)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MobileThemeColors.CocoaDarkText
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Ej. Centro, Alameda") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MobileThemeColors.IceCreamPink,
                            focusedTextColor = MobileThemeColors.CocoaDarkText,
                            unfocusedTextColor = MobileThemeColors.CocoaDarkText
                        ),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Button(
                        onClick = { performForwardGeocode(searchQuery) },
                        colors = ButtonDefaults.buttonColors(containerColor = MobileThemeColors.PinkText),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Text("Buscar", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }

                if (isLoading) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidth(),
                        color = MobileThemeColors.PinkText
                    )
                }

                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = Color.Red,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Divider(color = Color.LightGray.copy(alpha = 0.5f))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(MobileThemeColors.IceCreamPink, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📍", fontSize = 18.sp)
                    }
                    Column {
                        Text(
                            text = resolvedAddress,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MobileThemeColors.CocoaDarkText,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Lat: ${String.format("%.4f", latitude)} | Lng: ${String.format("%.4f", longitude)}",
                            fontSize = 11.sp,
                            color = MobileThemeColors.CocoaLightText
                        )
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = MobileThemeColors.PureWhiteCard),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, MobileThemeColors.IceCreamPink),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier
                .weight(1f)
                .fillMaxSize()
        ) {
            AndroidView(
                factory = { ctx ->
                    android.webkit.WebView(ctx).apply {
                        layoutParams = android.view.ViewGroup.LayoutParams(
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            android.view.ViewGroup.LayoutParams.MATCH_PARENT
                        )
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.databaseEnabled = true
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                        webViewClient = android.webkit.WebViewClient()
                        webChromeClient = android.webkit.WebChromeClient()
                    }
                },
                update = { webView ->
                    val shopCoords = mapOf(
                        "nev_los_abuelos" to Pair(21.1565, -100.9312),
                        "nev_la_mich" to Pair(21.1585, -100.9300),
                        "nev_zero" to Pair(21.1640, -100.9350),
                        "nev_artis" to Pair(21.1400, -100.9150),
                        "nev_far" to Pair(21.1200, -100.9500),
                        "nev_centenario" to Pair(21.1750, -100.9200),
                        "nev_gelato" to Pair(21.1850, -100.9400),
                        "nev_antonio" to Pair(21.1500, -100.9450),
                        "nev_copo" to Pair(21.1450, -100.9200),
                        "nev_flor" to Pair(21.2200, -100.9000)
                    )

                    val markersJs = StringBuilder()
                    shops.forEach { shop ->
                        val coords = shopCoords[shop.id] ?: Pair(21.1561, -100.9312)
                        val titleEscaped = shop.nombre.replace("'", "\\'")
                        val descEscaped = "Horario: ${shop.horario.replace("'", "\\'")}\\nDirección: ${shop.direccion.replace("'", "\\'")}"
                        markersJs.append("""
                            L.marker([${coords.first}, ${coords.second}], {icon: iceCreamIcon})
                                .addTo(map)
                                .bindPopup("<b>🍦 $titleEscaped</b><br>$descEscaped");
                        """.trimIndent() + "\n")
                    }

                    val resolvedAddressEscaped = resolvedAddress
                        .replace("\\", "\\\\")
                        .replace("'", "\\'")
                        .replace("\"", "\\\"")
                        .replace("\n", " ")
                        .replace("\r", " ")

                    val htmlContent = """
                        <!DOCTYPE html>
                        <html>
                        <head>
                            <meta charset="utf-8" />
                            <meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
                            <link rel="stylesheet" href="https://unpkg.com/leaflet@1.7.1/dist/leaflet.css" />
                            <script src="https://unpkg.com/leaflet@1.7.1/dist/leaflet.js"></script>
                            <style>
                                html, body {
                                    width: 100%;
                                    height: 100%;
                                    margin: 0;
                                    padding: 0;
                                }
                                #map {
                                    width: 100vw;
                                    height: 100vh;
                                }
                            </style>
                        </head>
                        <body>
                            <div id="map"></div>
                            <script>
                                var map = L.map('map').setView([$latitude, $longitude], 14);
                                L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
                                    attribution: '© OpenStreetMap'
                                }).addTo(map);
                                
                                var redIcon = L.icon({
                                    iconUrl: 'https://raw.githubusercontent.com/pointhi/leaflet-color-markers/master/img/marker-icon-red.png',
                                    shadowUrl: 'https://cdnjs.cloudflare.com/ajax/libs/leaflet/0.7.7/images/marker-shadow.png',
                                    iconSize: [25, 41],
                                    iconAnchor: [12, 41],
                                    popupAnchor: [1, -34],
                                    shadowSize: [41, 41]
                                });
                                
                                var iceCreamIcon = L.divIcon({
                                    html: '<div style="background-color: white; border: 2.5px solid #EF9A9A; border-radius: 50%; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; box-shadow: 0 2px 6px rgba(0,0,0,0.3); font-size: 20px;">🍦</div>',
                                    className: 'custom-div-icon',
                                    iconSize: [36, 36],
                                    iconAnchor: [18, 18],
                                    popupAnchor: [0, -18]
                                });
                                L.marker([$latitude, $longitude], {icon: redIcon}).addTo(map)
                                    .bindPopup("<b>📍 Ubicación Buscada</b><br>$resolvedAddressEscaped")
                                    .openPopup();
                                
                                $markersJs
                            </script>
                        </body>
                        </html>
                    """.trimIndent()
                    webView.loadDataWithBaseURL("https://unpkg.com/", htmlContent, "text/html", "utf-8", null)
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

@Composable
fun AdminOrdersHistoryTab(
    repository: SnowTrailRepository,
    reloadFromDb: () -> Unit,
    triggerSync: (String, String?, String?) -> Unit = { _, _, _ -> }
) {
    var allOrders by remember { mutableStateOf(emptyList<MockOrder>()) }
    
    fun refreshOrders() {
        allOrders = repository.getAllOrders()
    }
    
    LaunchedEffect(Unit) {
        refreshOrders()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "📋 Gestión de Pedidos por Usuario", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                    
                    TextButton(
                        onClick = {
                            repository.clearOrdersHistory()
                            refreshOrders()
                            reloadFromDb()
                            triggerSync("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                        }
                    ) {
                        Text("Borrar Todo", color = Color.Red, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))
                
                if (allOrders.isNotEmpty()) {
                    val grouped = allOrders.groupBy { it.userEmail }
                    
                    grouped.forEach { (email, ordersGroup) ->
                        Text(
                            text = "👤 Usuario: $email",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFFEF9A9A),
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                        
                        ordersGroup.forEach { o ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFFFAFAFA)),
                                border = BorderStroke(1.dp, Color(0xFFE0E0E0)),
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)
                            ) {
                                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = "Pedido: #${o.id}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF333333))
                                        
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    when (o.estado) {
                                                        "NUEVO" -> Color(0xFFFFF9C4)
                                                        "ACEPTADO" -> Color(0xFFE8F5E9)
                                                        "POSPUESTO" -> Color(0xFFFFE0B2)
                                                        "ENTREGADO" -> Color(0xFFE3F2FD)
                                                        "RECHAZADO" -> Color(0xFFFFEBEE)
                                                        else -> Color(0xFFF5F5F5)
                                                    },
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .border(
                                                    1.dp,
                                                    when (o.estado) {
                                                        "NUEVO" -> Color(0xFFF57F17)
                                                        "ACEPTADO" -> Color(0xFF2E7D32)
                                                        "POSPUESTO" -> Color(0xFFE65100)
                                                        "ENTREGADO" -> Color(0xFF1565C0)
                                                        "RECHAZADO" -> Color(0xFFC62828)
                                                        else -> Color.Gray
                                                    },
                                                    RoundedCornerShape(8.dp)
                                                )
                                                .padding(horizontal = 8.dp, vertical = 3.dp)
                                        ) {
                                            Text(
                                                text = o.estado,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = when (o.estado) {
                                                    "NUEVO" -> Color(0xFFF57F17)
                                                    "ACEPTADO" -> Color(0xFF2E7D32)
                                                    "POSPUESTO" -> Color(0xFFE65100)
                                                    "ENTREGADO" -> Color(0xFF1565C0)
                                                    "RECHAZADO" -> Color(0xFFC62828)
                                                    else -> Color.DarkGray
                                                }
                                            )
                                        }
                                    }
                                    
                                    Text(text = "🏪 Nevería: ${o.neveriaNombre}", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF424242))
                                    
                                    val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm", java.util.Locale.getDefault())
                                    val dateStr = sdf.format(java.util.Date(o.fechaHoraMillis))
                                    Text(text = "📅 Fecha: $dateStr", fontSize = 11.sp, color = Color.Gray)
                                    
                                    val prodStr = o.productos.joinToString(", ") { "${it.cantidad}x ${it.nombre}" }
                                    Text(text = "🛍️ Items: $prodStr", fontSize = 11.sp, color = Color.DarkGray)
                                    Text(text = "💵 Total: $${o.total}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFFB52D5E))
                                    
                                    Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 4.dp))
                                    
                                    Text(text = "Cambiar Estado del Pedido (Admin):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF757575))
                                    
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Aceptar
                                            Button(
                                                onClick = {
                                                    repository.updateOrderStatus(o.id, "ACEPTADO")
                                                    refreshOrders()
                                                    reloadFromDb()
                                                    triggerSync("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE8F5E9)),
                                                border = BorderStroke(1.dp, Color(0xFF4CAF50)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Aceptar", color = Color(0xFF2E7D32), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            
                                            // Posponer
                                            Button(
                                                onClick = {
                                                    repository.updateOrderStatus(o.id, "POSPUESTO")
                                                    refreshOrders()
                                                    reloadFromDb()
                                                    triggerSync("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFE0B2)),
                                                border = BorderStroke(1.dp, Color(0xFFFF9800)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Posponer", color = Color(0xFFE65100), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                        
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            // Entregar
                                            Button(
                                                onClick = {
                                                    repository.updateOrderStatus(o.id, "ENTREGADO")
                                                    refreshOrders()
                                                    reloadFromDb()
                                                    triggerSync("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE3F2FD)),
                                                border = BorderStroke(1.dp, Color(0xFF2196F3)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Entregar", color = Color(0xFF1565C0), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                            
                                            // Rechazar
                                            Button(
                                                onClick = {
                                                    repository.updateOrderStatus(o.id, "RECHAZADO")
                                                    refreshOrders()
                                                    reloadFromDb()
                                                    triggerSync("mx.utng.snowtrail.ACTION_SYNC_ORDER", null, null)
                                                },
                                                modifier = Modifier.weight(1f).height(36.dp),
                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFFEBEE)),
                                                border = BorderStroke(1.dp, Color(0xFFF44336)),
                                                shape = RoundedCornerShape(10.dp),
                                                contentPadding = PaddingValues(0.dp)
                                            ) {
                                                Text("Rechazar", color = Color(0xFFC62828), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        Divider(color = Color(0xFFEEEEEE), modifier = Modifier.padding(vertical = 6.dp))
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("No hay pedidos registrados en el historial.", fontSize = 14.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}


