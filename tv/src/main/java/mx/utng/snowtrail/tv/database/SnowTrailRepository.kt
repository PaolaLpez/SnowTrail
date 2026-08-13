package mx.utng.snowtrail.tv.database

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

/**
 * ARCHIVO: SnowTrailRepository.kt
 * PROPÓSITO: Repositorio de datos para Android TV (`:tv`).
 * Ofrece métodos relacionales para almacenar y consultar promociones recibidas vía TCP Socket y gestionar la cola de comandera.
 */
class SnowTrailRepository(context: Context) {
    private val dbHelper = DatabaseHelper(context)

    /**
     * Modelo de Promoción para Android TV.
     */
    data class TvPromotion(
        val id: String,
        val nombre: String,
        val fechaInicio: String,
        val fechaFin: String,
        val nota: String,
        val imagen: String,
        val neveriaId: String
    )

    /**
     * Modelo de Pedido para la comandera digital de Android TV.
     */
    data class TvOrder(
        val id: String,
        val cliente: String,
        val paraRecoger: String,
        val tiempoEntrega: String,
        val total: String,
        val items: String,
        val estado: String,
        val neveriaId: String
    )

    /**
     * Consulta todas las promociones almacenadas en SQLite para la marquesina de TV.
     */
    fun getPromotions(): List<TvPromotion> {
        val list = mutableListOf<TvPromotion>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_PROMOTIONS}", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_NAME))
                val start = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_START))
                val end = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_END))
                val note = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_NOTE))
                val img = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_IMAGE))
                val shopId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_SHOP_ID)) ?: "nev_los_abuelos"
                list.add(TvPromotion(id, nombre, start, end, note, img, shopId))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun getOrders(): List<TvOrder> {
        val list = mutableListOf<TvOrder>()
        val db = dbHelper.readableDatabase
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_ORDERS}", null)
        if (cursor.moveToFirst()) {
            do {
                val id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_ID))
                val cliente = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_CLIENT))
                val pickup = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_PICKUP))
                val eta = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_ETA))
                val total = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_TOTAL))
                val items = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_ITEMS))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_STATUS))
                val shopId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_SHOP_ID)) ?: "nev_los_abuelos"
                list.add(TvOrder(id, cliente, pickup, eta, total, items, estado, shopId))
            } while (cursor.moveToNext())
        }
        cursor.close()
        return list
    }

    fun updateOrderStatus(orderId: String, newStatus: String) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.ORDER_STATUS, newStatus)
        }
        db.update(DatabaseHelper.TABLE_ORDERS, values, "${DatabaseHelper.ORDER_ID} = ?", arrayOf(orderId))
    }

    fun savePromotion(promo: TvPromotion) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.PROMO_ID, promo.id)
            put(DatabaseHelper.PROMO_NAME, promo.nombre)
            put(DatabaseHelper.PROMO_START, promo.fechaInicio)
            put(DatabaseHelper.PROMO_END, promo.fechaFin)
            put(DatabaseHelper.PROMO_NOTE, promo.nota)
            put(DatabaseHelper.PROMO_IMAGE, promo.imagen)
            put(DatabaseHelper.PROMO_SHOP_ID, promo.neveriaId)
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_PROMOTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun saveOrder(order: TvOrder) {
        val db = dbHelper.writableDatabase
        val values = ContentValues().apply {
            put(DatabaseHelper.ORDER_ID, order.id)
            put(DatabaseHelper.ORDER_CLIENT, order.cliente)
            put(DatabaseHelper.ORDER_PICKUP, order.paraRecoger)
            put(DatabaseHelper.ORDER_ETA, order.tiempoEntrega)
            put(DatabaseHelper.ORDER_TOTAL, order.total)
            put(DatabaseHelper.ORDER_ITEMS, order.items)
            put(DatabaseHelper.ORDER_STATUS, order.estado)
            put(DatabaseHelper.ORDER_SHOP_ID, order.neveriaId)
        }
        db.insertWithOnConflict(DatabaseHelper.TABLE_ORDERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
