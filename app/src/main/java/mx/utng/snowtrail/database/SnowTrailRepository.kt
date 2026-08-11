package mx.utng.snowtrail.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import mx.utng.snowtrail.service.MockShop
import mx.utng.snowtrail.service.MockOrder
import mx.utng.snowtrail.service.MockNotification
import mx.utng.snowtrail.service.MockProductLine
import mx.utng.snowtrail.service.MockPromotion

class SnowTrailRepository(context: Context) {

    private val dbHelper = DatabaseHelper(context)

    // --- SHOPS OPERATIONS ---

    fun getShops(): List<MockShop> {
        val shops = mutableListOf<MockShop>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_SHOPS}", null)
        
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.SHOP_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.SHOP_NAME))
                val distancia = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.SHOP_DISTANCE))
                val esFavorita = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.SHOP_FAVORITE)) == 1
                val tienePromocion = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.SHOP_PROMOTION)) == 1
                val horario = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.SHOP_HORARIO)) ?: "9:00 AM - 9:00 PM"
                val contacto = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.SHOP_CONTACTO)) ?: "55 1234 5678, correo@neveria.com"
                val direccion = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.SHOP_DIRECCION)) ?: "Av. Principal 123"
                shops.add(MockShop(id, nombre, distancia, esFavorita, tienePromocion, horario, contacto, direccion))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return shops
    }

    fun saveShop(shop: MockShop) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.SHOP_ID, shop.id)
            put(DatabaseHelper.SHOP_NAME, shop.nombre)
            put(DatabaseHelper.SHOP_DISTANCE, shop.distancia)
            put(DatabaseHelper.SHOP_FAVORITE, if (shop.esFavorita) 1 else 0)
            put(DatabaseHelper.SHOP_PROMOTION, if (shop.tienePromocion) 1 else 0)
            put(DatabaseHelper.SHOP_HORARIO, shop.horario)
            put(DatabaseHelper.SHOP_CONTACTO, shop.contacto)
            put(DatabaseHelper.SHOP_DIRECCION, shop.direccion)
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_SHOPS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deleteShop(shopId: String): Boolean {
        val db = dbHelper.writableDatabase
        val result = db.delete(DatabaseHelper.TABLE_SHOPS, "${DatabaseHelper.SHOP_ID} = ?", arrayOf(shopId))
        return result > 0
    }

    fun updateShopDistance(shopId: String, newDistance: Double) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.SHOP_DISTANCE, newDistance)
        }
        db.update(DatabaseHelper.TABLE_SHOPS, values, "${DatabaseHelper.SHOP_ID} = ?", arrayOf(shopId))
    }

    fun toggleFavoriteShop(shopId: String): Boolean {
        val db = dbHelper.writableDatabase
        var isFavorite = false
        
        db.beginTransaction()
        try {
            val cursor = db.rawQuery(
                "SELECT ${DatabaseHelper.SHOP_FAVORITE} FROM ${DatabaseHelper.TABLE_SHOPS} WHERE ${DatabaseHelper.SHOP_ID} = ?",
                arrayOf(shopId)
            )
            if (cursor.moveToFirst()) {
                val current = cursor.getInt(0)
                isFavorite = current == 0
                val values = ContentValues().apply {
                    put(DatabaseHelper.SHOP_FAVORITE, if (isFavorite) 1 else 0)
                }
                db.update(DatabaseHelper.TABLE_SHOPS, values, "${DatabaseHelper.SHOP_ID} = ?", arrayOf(shopId))
            }
            cursor.close()
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        return isFavorite
    }

    // --- ORDERS OPERATIONS ---

    fun getActiveOrder(): MockOrder? {
        val db = dbHelper.readableDatabase
        // Get the most recent active order that is not RECHAZADO or ENTREGADO
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_ORDERS} WHERE ${DatabaseHelper.ORDER_STATUS} NOT IN ('RECHAZADO', 'ENTREGADO') ORDER BY ${DatabaseHelper.ORDER_TIMESTAMP} DESC LIMIT 1",
            null
        )
        
        var order: MockOrder? = null
        if (cursor.moveToFirst()) {
            val orderId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_ID))
            val neveriaId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_SHOP_ID))
            val neveriaNombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_SHOP_NAME))
            val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_STATUS))
            val tiempoEstimado = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_ETA))
            val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_TIMESTAMP))
            val total = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_TOTAL))
            
            val userEmailCol = cursor.getColumnIndex("user_email")
            val userEmail = if (userEmailCol != -1) cursor.getString(userEmailCol) ?: "Cliente@gmail.com" else "Cliente@gmail.com"
            
            val products = getOrderProducts(orderId)
            order = MockOrder(orderId, neveriaId, neveriaNombre, estado, tiempoEstimado, timestamp, total, products, userEmail)
        }
        cursor.close()
        return order
    }

    private fun getOrderProducts(orderId: String): List<MockProductLine> {
        val products = mutableListOf<MockProductLine>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_ORDER_PRODUCTS} WHERE ${DatabaseHelper.PROD_ORDER_ID} = ?",
            arrayOf(orderId)
        )
        
        if (cursor.moveToFirst()) {
            do {
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROD_NAME))
                val cantidad = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.PROD_QTY))
                val precio = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.PROD_PRICE))
                products.add(MockProductLine(nombre, cantidad, precio))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return products
    }

    fun saveOrder(order: MockOrder) {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put(DatabaseHelper.ORDER_ID, order.id)
                put(DatabaseHelper.ORDER_SHOP_ID, order.neveriaId)
                put(DatabaseHelper.ORDER_SHOP_NAME, order.neveriaNombre)
                put(DatabaseHelper.ORDER_STATUS, order.estado)
                put(DatabaseHelper.ORDER_ETA, order.tiempoEstimadoMinutos)
                put(DatabaseHelper.ORDER_TIMESTAMP, order.fechaHoraMillis)
                put(DatabaseHelper.ORDER_TOTAL, order.total)
                put("user_email", order.userEmail)
            }
            db.insertWithOnConflict(DatabaseHelper.TABLE_ORDERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
            
            // Delete old products if updating
            db.delete(DatabaseHelper.TABLE_ORDER_PRODUCTS, "${DatabaseHelper.PROD_ORDER_ID} = ?", arrayOf(order.id))
            
            // Insert products
            for (prod in order.productos) {
                val pValues = ContentValues().apply {
                    put(DatabaseHelper.PROD_ORDER_ID, order.id)
                    put(DatabaseHelper.PROD_NAME, prod.nombre)
                    put(DatabaseHelper.PROD_QTY, prod.cantidad)
                    put(DatabaseHelper.PROD_PRICE, prod.precioUnitario)
                }
                db.insert(DatabaseHelper.TABLE_ORDER_PRODUCTS, null, pValues)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    fun getAllOrders(): List<MockOrder> {
        val ordersList = mutableListOf<MockOrder>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_ORDERS} ORDER BY ${DatabaseHelper.ORDER_TIMESTAMP} DESC",
            null
        )
        if (cursor.moveToFirst()) {
            do {
                val orderId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_ID))
                val neveriaId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_SHOP_ID))
                val neveriaNombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_SHOP_NAME))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_STATUS))
                val tiempoEstimado = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_ETA))
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_TIMESTAMP))
                val total = cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_TOTAL))
                
                val userEmailCol = cursor.getColumnIndex("user_email")
                val userEmail = if (userEmailCol != -1) cursor.getString(userEmailCol) ?: "Cliente@gmail.com" else "Cliente@gmail.com"
                
                val products = getOrderProducts(orderId)
                ordersList.add(MockOrder(orderId, neveriaId, neveriaNombre, estado, tiempoEstimado, timestamp, total, products, userEmail))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return ordersList
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.ORDER_STATUS, newStatus)
        }
        db.update(DatabaseHelper.TABLE_ORDERS, values, "${DatabaseHelper.ORDER_ID} = ?", arrayOf(orderId))
    }

    // --- NOTIFICATIONS OPERATIONS ---

    fun getNotifications(): List<MockNotification> {
        val notifs = mutableListOf<MockNotification>()
        val db = dbHelper.readableDatabase
        val cursor: Cursor = db.rawQuery(
            "SELECT * FROM ${DatabaseHelper.TABLE_NOTIFICATIONS} ORDER BY ${DatabaseHelper.NOTIF_TIMESTAMP} DESC",
            null
        )
        
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.NOTIF_ID))
                val mensaje = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.NOTIF_MESSAGE))
                val tipo = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.NOTIF_TYPE))
                val leida = cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.NOTIF_READ)) == 1
                val timestamp = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.NOTIF_TIMESTAMP))
                notifs.add(MockNotification(id, mensaje, tipo, leida, timestamp))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return notifs
    }

    fun addNotification(notif: MockNotification) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.NOTIF_ID, notif.id)
            put(DatabaseHelper.NOTIF_MESSAGE, notif.mensaje)
            put(DatabaseHelper.NOTIF_TYPE, notif.tipo)
            put(DatabaseHelper.NOTIF_READ, if (notif.leida) 1 else 0)
            put(DatabaseHelper.NOTIF_TIMESTAMP, notif.fechaEnvio)
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_NOTIFICATIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun markNotificationRead(notifId: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.NOTIF_READ, 1)
        }
        db.update(DatabaseHelper.TABLE_NOTIFICATIONS, values, "${DatabaseHelper.NOTIF_ID} = ?", arrayOf(notifId))
    }

    fun deleteNotification(notifId: String) {
        val db = dbHelper.writableDatabase
        db.delete(DatabaseHelper.TABLE_NOTIFICATIONS, "${DatabaseHelper.NOTIF_ID} = ?", arrayOf(notifId))
    }

    // --- INITIALIZATION ---

    fun initializeDemoDataIfEmpty() {
        val db = dbHelper.writableDatabase
        // Clear and re-populate to ensure all 10 shops are present
        db.delete(DatabaseHelper.TABLE_SHOPS, null, null)
        
        val initialShops = listOf(
            MockShop("nev_los_abuelos", "Los Abuelos", 80.0, true, true, "Lun-Vie 9-23h, Sáb-Dom 10-24h", "55 1234 5678, correo@neveria.com", "Avd. Abricación De Aguaa, 154"),
            MockShop("nev_la_mich", "La Michoacana", 350.0, true, false, "Lun-Dom 10:00 - 22:00", "418 987 6543, michoacana@gmail.com", "Calle Hidalgo #45, Centro"),
            MockShop("nev_zero", "Helados Bajo Cero", 1200.0, false, true, "Lun-Sáb 11:00 - 21:00", "418 111 2222, bajocero@gmail.com", "Av. Principal #320"),
            MockShop("nev_artis", "Artesanales del Valle", 2900.0, false, false, "Lun-Dom 12:00 - 20:00", "418 333 4444, info@valle.com", "Paseo de la Loma #78"),
            MockShop("nev_far", "Heladería Lejana", 4500.0, false, false, "Mar-Dom 11:00 - 19:00", "418 555 6666, far@helado.com", "Camino Real Km 5"),
            MockShop("nev_centenario", "Nieves del Centenario", 2800.0, false, false, "Lun-Dom 9:00 - 22:00", "418 777 8888, centenario@nieves.com", "Plaza Principal #3"),
            MockShop("nev_gelato", "Gelato Italiano", 3800.0, false, true, "Lun-Dom 11:00 - 23:00", "418 888 9999, gelato@italiano.com", "Boulevard Tulipanes #890"),
            MockShop("nev_antonio", "Paletería San Antonio", 1800.0, false, false, "Lun-Sáb 10:00 - 21:00", "418 222 3333, antoniopaletas@gmail.com", "San Antonio #12"),
            MockShop("nev_copo", "El Copo Dorado", 2600.0, false, false, "Lun-Dom 10:00 - 22:00", "418 444 5555, copo@dorado.com", "Calzada de la Estación #104"),
            MockShop("nev_flor", "Flor de Dolores", 8000.0, false, false, "Lun-Dom 9:00 - 21:00", "418 666 7777, flordelores@outlook.com", "Av. Guanajuato #12")
        )
        for (shop in initialShops) {
            saveShop(shop)
        }

        db.delete(DatabaseHelper.TABLE_PROMOTIONS, null, null)
        val initialPromos = listOf(
            MockPromotion("pa1", "2x1 en Helados de Fruta", "2026-08-01", "2026-08-30", "Aplica en todos los sabores naturales"),
            MockPromotion("pa2", "Copa Suprema de Fresa y Nueces", "2026-08-01", "2026-08-30", "25% de descuento los fines de semana"),
            MockPromotion("pa3", "Nieve de Limón Gigante", "2026-08-01", "2026-08-31", "Nieve tradicional estilo Dolores"),
            MockPromotion("pa4", "Malteada Clásica de Vainilla", "2026-08-05", "2026-08-25", "Lunes a jueves a mitad de precio"),
            MockPromotion("pa5", "Paleta de Cajeta Quemada", "2026-08-01", "2026-08-20", "Prueba la receta original de la casa"),

            MockPromotion("pm1", "Brownie Split Deluxe: \$59 MXN", "2026-08-01", "2026-08-31", "Con bola de vainilla y fudge caliente"),
            MockPromotion("pm2", "Combo 3 Paletas de Agua", "2026-08-01", "2026-08-31", "Por solo \$35 pesos en sabores frutales"),
            MockPromotion("pm3", "Litro de Helado Combinado", "2026-08-05", "2026-08-30", "Llévate el segundo litro con 30% desc."),
            MockPromotion("pm4", "Mangonada Extrema con Chamoy", "2026-08-01", "2026-08-15", "Con gomitas y chilito en polvo"),
            MockPromotion("pm5", "Paleta de Crema Oreo", "2026-08-01", "2026-08-20", "Paleta rellena de galleta oreo crujiente"),

            MockPromotion("pz1", "¡Combo 10 Paletas x \$199!", "2026-08-01", "2026-08-30", "Paletas de crema o agua medianas"),
            MockPromotion("pz2", "Waffle con Nieve de Fresa", "2026-08-01", "2026-08-31", "Bañado en chocolate belga caliente"),
            MockPromotion("pz3", "Smoothie Loco de Mango y Piña", "2026-08-01", "2026-08-20", "Bebida helada ultra refrescante"),
            MockPromotion("pz4", "Helado Vegano de Coco", "2026-08-05", "2026-08-25", "Hecho con leche de coco 100% natural"),
            MockPromotion("pz5", "Paleta de Chicle y Malvaviscos", "2026-08-01", "2026-08-15", "La favorita de los pequeños de la casa"),

            MockPromotion("pr1", "Helado de Lavanda y Miel", "2026-08-01", "2026-08-31", "Sabor exclusivo artesanal de temporada"),
            MockPromotion("pr2", "Cono de Mezcal con Naranja", "2026-08-01", "2026-08-15", "Solo para adultos, receta tradicional"),
            MockPromotion("pr3", "Tarta Helado Queso-Zarzamora", "2026-08-05", "2026-08-25", "Rebanada individual al 3x2"),
            MockPromotion("pr4", "Nieve de Garambullo Orgánica", "2026-08-01", "2026-08-30", "Fruto típico de la región de Dolores"),
            MockPromotion("pr5", "Paleta Yogurt con Frutos Rojos", "2026-08-01", "2026-08-20", "Baja en calorías y sin azúcar añadida"),

            MockPromotion("pf1", "Helado de Chocolate Abuelita", "2026-08-01", "2026-08-31", "Con un toque de canela y trozos de chocolate"),
            MockPromotion("pf2", "Paleta Helada de Rompope", "2026-08-01", "2026-08-20", "Con trocitos de nuez pecana selecta"),
            MockPromotion("pf3", "Nieve de Nopal con Piña", "2026-08-05", "2026-08-25", "Súper fresca y digestiva, pruébala hoy"),
            MockPromotion("pf4", "Flotante de Refresco con Vainilla", "2026-08-01", "2026-08-30", "Bebida retro con bola de nieve cremosa"),
            MockPromotion("pf5", "Sándwich de Helado Gigante", "2026-08-01", "2026-08-15", "Galletas caseras rellenas de helado"),

            MockPromotion("pc1", "Nieve Histórica de Tres Leches", "2026-08-01", "2026-08-31", "Celebrando 100 años del sabor de Dolores"),
            MockPromotion("pc2", "Copa Centenario Tricolor", "2026-08-01", "2026-08-15", "Nieve de limón, guanábana y fresa"),
            MockPromotion("pc3", "Cono de Chocolate Amargo", "2026-08-05", "2026-08-25", "70% cacao mexicano orgánico del sur"),
            MockPromotion("pc4", "Paleta de Cajeta con Nuez", "2026-08-01", "2026-08-30", "Tradición familiar desde hace décadas"),
            MockPromotion("pc5", "Nieve de Queso de Cabra con Higo", "2026-08-01", "2026-08-20", "Sabor gourmet e inigualable en Dolores"),

            MockPromotion("pg1", "Gelato de Pistacho de Bronte", "2026-08-01", "2026-08-31", "Con pistachos italianos importados y tostados"),
            MockPromotion("pg2", "Affogato de Expreso con Gelato", "2026-08-01", "2026-08-20", "Café caliente servido sobre vainilla"),
            MockPromotion("pg3", "Gelato Stracciatella Crujiente", "2026-08-05", "2026-08-25", "Base de crema con finos hilos de chocolate"),
            MockPromotion("pg4", "Sorbetto de Limone di Sicilia", "2026-08-01", "2026-08-30", "Sin lactosa, ultra fresco y natural"),
            MockPromotion("pg5", "Gelato de Avellana Piamonte", "2026-08-01", "2026-08-15", "Crema italiana clásica con chocolate y avellana"),

            MockPromotion("pn1", "Esquimal de Fresa con Chocolate", "2026-08-01", "2026-08-31", "Cubierta crujiente y coco rallado de topping"),
            MockPromotion("pn2", "Paleta San Antonio de Kiwi", "2026-08-01", "2026-08-15", "Kiwi natural rebanado dentro de la paleta"),
            MockPromotion("pn3", "Vaso Helado Vainilla con Chispas", "2026-08-05", "2026-08-25", "Ideal para niños, llévate el segundo gratis"),
            MockPromotion("pn4", "Paleta Tamarindo Rellena Chamoy", "2026-08-01", "2026-08-30", "Picosa y dulce, un antojo perfecto"),
            MockPromotion("pn5", "Helado Doble Menta con Chocolate", "2026-08-01", "2026-08-20", "Gran frescura y sabor a chocolate belga"),

            MockPromotion("pd1", "Copo de Oro de Mango con Tajín", "2026-08-01", "2026-08-31", "Servido en copa con chile y serpentinas"),
            MockPromotion("pd2", "Raspado Grosella con Condensada", "2026-08-01", "2026-08-20", "Tradicional raspado de hielo picado"),
            MockPromotion("pd3", "Helado Plátano y Toffee", "2026-08-05", "2026-08-25", "Con trocitos de nuez y salsa de caramelo"),
            MockPromotion("pd4", "Paleta de Guanábana Cremosa", "2026-08-01", "2026-08-30", "Fruta fresca de temporada seleccionada"),
            MockPromotion("pd5", "Copa de Helado de Arándanos", "2026-08-01", "2026-08-15", "Con crema batida y arándanos silvestres"),

            MockPromotion("pl1", "Nieve de Rosas con Almendras", "2026-08-01", "2026-08-31", "Sabor histórico emblemático con pétalos de rosa"),
            MockPromotion("pl2", "Helado de Flor de Naranjo", "2026-08-01", "2026-08-15", "Sabor sutil y aromático de primavera"),
            MockPromotion("pl3", "Nieve Tequila con Sal y Limón", "2026-08-05", "2026-08-25", "Sabor tradicional e inigualable de la feria"),
            MockPromotion("pl4", "Paleta de Mango, Fresa y Limón", "2026-08-01", "2026-08-30", "Tres deliciosos colores y sabores naturales"),
            MockPromotion("pl5", "Helado Vainilla Papantla Premium", "2026-08-01", "2026-08-20", "Con vaina de vainilla mexicana auténtica")
        )
        for (promo in initialPromos) {
            savePromotion(promo)
        }
        
        val cursorNotifs = db.rawQuery("SELECT COUNT(*) FROM ${DatabaseHelper.TABLE_NOTIFICATIONS}", null)
        var notifCount = 0L
        if (cursorNotifs.moveToFirst()) {
            notifCount = cursorNotifs.getLong(0)
        }
        cursorNotifs.close()
        
        if (notifCount == 0L) {
            val initialNotifs = listOf(
                MockNotification("notif_1", "Tu pedido ha sido creado", "CAMBIO_ESTADO", false, System.currentTimeMillis() - 600000),
                MockNotification("notif_2", "¡Promoción 2x1 en nieve de fresa!", "PROMOCION", false, System.currentTimeMillis() - 1200000),
                MockNotification("notif_3", "Estás cerca de Helados Bajo Cero", "PROXIMIDAD", true, System.currentTimeMillis() - 3600000)
            )
            for (notif in initialNotifs) {
                addNotification(notif)
            }
        }
        
        // Seed Admin user if database didn't trigger onCreate upgrades cleanly
        seedAdminUser()
    }

    fun registerUser(email: String, password: String, role: String): Boolean {
        val db = dbHelper.writableDatabase
        var exists = false
        val cursor = db.rawQuery(
            "SELECT 1 FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.USER_EMAIL} = ?",
            arrayOf(email)
        )
        if (cursor.moveToFirst()) {
            exists = true
        }
        cursor.close()
        
        if (exists) return false

        val values = ContentValues().apply {
            put(DatabaseHelper.USER_EMAIL, email)
            put(DatabaseHelper.USER_PASSWORD, password)
            put(DatabaseHelper.USER_ROLE, role)
        }
        val result = db.insert(DatabaseHelper.TABLE_USERS, null, values)
        return result != -1L
    }

    fun authenticateUser(email: String, password: String): String? {
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery(
            "SELECT ${DatabaseHelper.USER_ROLE} FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.USER_EMAIL} = ? AND ${DatabaseHelper.USER_PASSWORD} = ?",
            arrayOf(email, password)
        )
        var role: String? = null
        if (cursor.moveToFirst()) {
            role = cursor.getString(0)
        }
        cursor.close()
        return role
    }

    fun seedAdminUser() {
        val db = dbHelper.writableDatabase
        var exists = false
        val cursor = db.rawQuery(
            "SELECT 1 FROM ${DatabaseHelper.TABLE_USERS} WHERE ${DatabaseHelper.USER_EMAIL} = ?",
            arrayOf("Admin@gmail.com")
        )
        if (cursor.moveToFirst()) {
            exists = true
        }
        cursor.close()

        if (!exists) {
            val values = ContentValues().apply {
                put(DatabaseHelper.USER_EMAIL, "Admin@gmail.com")
                put(DatabaseHelper.USER_PASSWORD, "admin123")
                put(DatabaseHelper.USER_ROLE, "ADMIN")
            }
            db.insert(DatabaseHelper.TABLE_USERS, null, values)
        }
    }

    fun getPromotions(): List<MockPromotion> {
        val promotions = mutableListOf<MockPromotion>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_PROMOTIONS}", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_NAME))
                val start = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_START))
                val end = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_END))
                val note = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_NOTE))
                promotions.add(MockPromotion(id, nombre, start, end, note))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return promotions
    }

    fun savePromotion(promo: MockPromotion) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.PROMO_ID, promo.id)
            put(DatabaseHelper.PROMO_NAME, promo.nombre)
            put(DatabaseHelper.PROMO_START, promo.fechaInicio)
            put(DatabaseHelper.PROMO_END, promo.fechaFin)
            put(DatabaseHelper.PROMO_NOTE, promo.nota)
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_PROMOTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun deletePromotion(promoId: String): Boolean {
        val db = dbHelper.writableDatabase
        val result = db.delete(DatabaseHelper.TABLE_PROMOTIONS, "${DatabaseHelper.PROMO_ID} = ?", arrayOf(promoId))
        return result > 0
    }

    fun clearOrdersHistory() {
        val db = dbHelper.writableDatabase
        db.beginTransaction()
        try {
            db.delete(DatabaseHelper.TABLE_ORDERS, null, null)
            db.delete(DatabaseHelper.TABLE_ORDER_PRODUCTS, null, null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }
}
