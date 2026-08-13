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
     * Consulta todas las promociones almacenadas en la base de datos local SQLite para exhibir en la marquesina de TV.
     * 
     * @return Lista de objetos TvPromotion cargados desde la tabla promotions.
     */
    fun getPromotions(): List<TvPromotion> {
        // Inicializa la lista acumuladora de promociones
        val list = mutableListOf<TvPromotion>()
        // Abre la base de datos en modo lectura accesible
        val db = dbHelper.readableDatabase
        // Ejecuta la consulta SQL pura SELECT * sobre la tabla de promociones
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_PROMOTIONS}", null)
        
        // Verifica si la consulta devolvió al menos una fila y mueve el puntero al primer registro
        if (cursor.moveToFirst()) {
            do {
                // Extrae cada columna utilizando su nombre mapeado en el contrato DatabaseHelper
                val id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_ID))
                val nombre = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_NAME))
                val start = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_START))
                val end = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_END))
                val note = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_NOTE))
                val img = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_IMAGE))
                val shopId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.PROMO_SHOP_ID)) ?: "nev_los_abuelos"
                
                // Construye el objeto inmutable TvPromotion y lo añade a la lista
                list.add(TvPromotion(id, nombre, start, end, note, img, shopId))
            } while (cursor.moveToNext())
        }
        // Cierra el cursor para liberar recursos de memoria del sistema
        cursor.close()
        return list
    }

    /**
     * Consulta la lista completa de pedidos registrados en la comandera digital de Android TV.
     * 
     * @return Lista de objetos TvOrder almacenados en la tabla orders.
     */
    fun getOrders(): List<TvOrder> {
        // Inicializa la lista acumuladora de pedidos
        val list = mutableListOf<TvOrder>()
        // Abre la base de datos SQLite en modo lectura
        val db = dbHelper.readableDatabase
        // Ejecuta la consulta SQL SELECT * sobre la tabla de órdenes
        val cursor = db.rawQuery("SELECT * FROM ${DatabaseHelper.TABLE_ORDERS}", null)
        
        // Mueve el cursor al inicio del resultado de la consulta
        if (cursor.moveToFirst()) {
            do {
                // Mapeo de columnas de la tabla a variables locales
                val id = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_ID))
                val cliente = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_CLIENT))
                val pickup = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_PICKUP))
                val eta = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_ETA))
                val total = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_TOTAL))
                val items = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_ITEMS))
                val estado = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_STATUS))
                val shopId = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.ORDER_SHOP_ID)) ?: "nev_los_abuelos"
                
                // Instancia el objeto TvOrder y lo agrega al listado
                list.add(TvOrder(id, cliente, pickup, eta, total, items, estado, shopId))
            } while (cursor.moveToNext())
        }
        // Libera la referencia del cursor
        cursor.close()
        return list
    }

    /**
     * Actualiza el estado de una orden en la comandera digital (NUEVO -> PENDIENTE / ENTREGADO / RECHAZADO).
     * 
     * @param orderId Folio identificador de la orden.
     * @param newStatus Nuevo estado deseado para la máquina de estados.
     */
    fun updateOrderStatus(orderId: String, newStatus: String) {
        // Abre la base de datos en modo escritura
        val db = dbHelper.writableDatabase
        // Prepara los valores a actualizar en la columna de estado
        val values = ContentValues().apply {
            put(DatabaseHelper.ORDER_STATUS, newStatus)
        }
        // Ejecuta la sentencia UPDATE filtrando por la clave primaria orderId
        db.update(DatabaseHelper.TABLE_ORDERS, values, "${DatabaseHelper.ORDER_ID} = ?", arrayOf(orderId))
    }

    /**
     * Inserta o actualiza una promoción recibida por trama TCP Socket en la base de datos de Android TV.
     * 
     * @param promo Objeto TvPromotion recibido desde el smartphone.
     */
    fun savePromotion(promo: TvPromotion) {
        // Abre la conexión en modo escritura para modificación de tablas
        val db = dbHelper.writableDatabase
        // Empaqueta los campos del objeto de promoción en la estructura ContentValues
        val values = ContentValues().apply {
            put(DatabaseHelper.PROMO_ID, promo.id)
            put(DatabaseHelper.PROMO_NAME, promo.nombre)
            put(DatabaseHelper.PROMO_START, promo.fechaInicio)
            put(DatabaseHelper.PROMO_END, promo.fechaFin)
            put(DatabaseHelper.PROMO_NOTE, promo.nota)
            put(DatabaseHelper.PROMO_IMAGE, promo.imagen)
            put(DatabaseHelper.PROMO_SHOP_ID, promo.neveriaId)
        }
        // Ejecuta la inserción con resolución de conflictos mediante reemplazo de registro (CONFLICT_REPLACE)
        db.insertWithOnConflict(DatabaseHelper.TABLE_PROMOTIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    /**
     * Inserta o actualiza un pedido entrante transmitido por red TCP desde el celular.
     * 
     * @param order Objeto TvOrder recibido vía Socket.
     */
    fun saveOrder(order: TvOrder) {
        // Abre la base de datos en modo escritura
        val db = dbHelper.writableDatabase
        // Mapea los atributos de la orden recibida a valores de contenido SQLite
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
        // Aplica INSERT u OVERWRITE si la orden ya existía en la comandera
        db.insertWithOnConflict(DatabaseHelper.TABLE_ORDERS, null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }
}
