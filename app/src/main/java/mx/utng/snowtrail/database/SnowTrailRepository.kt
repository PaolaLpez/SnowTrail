package mx.utng.snowtrail.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import mx.utng.snowtrail.service.MockShop
import mx.utng.snowtrail.service.MockOrder
import mx.utng.snowtrail.service.MockNotification
import mx.utng.snowtrail.service.MockProductLine

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
                shops.add(MockShop(id, nombre, distancia, esFavorita, tienePromocion))
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
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_SHOPS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
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
            
            val products = getOrderProducts(orderId)
            order = MockOrder(orderId, neveriaId, neveriaNombre, estado, tiempoEstimado, timestamp, total, products)
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
            MockShop("nev_los_abuelos", "Los Abuelos", 80.0, true, true),
            MockShop("nev_la_mich", "La Michoacana", 350.0, true, false),
            MockShop("nev_zero", "Helados Bajo Cero", 1200.0, false, true),
            MockShop("nev_artis", "Artesanales del Valle", 2900.0, false, false),
            MockShop("nev_far", "Heladería Lejana", 4500.0, false, false),
            MockShop("nev_centenario", "Nieves del Centenario", 2800.0, false, false),
            MockShop("nev_gelato", "Gelato Italiano", 3800.0, false, true),
            MockShop("nev_antonio", "Paletería San Antonio", 1800.0, false, false),
            MockShop("nev_copo", "El Copo Dorado", 2600.0, false, false),
            MockShop("nev_flor", "Flor de Dolores", 8000.0, false, false)
        )
        for (shop in initialShops) {
            saveShop(shop)
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
    }
}
