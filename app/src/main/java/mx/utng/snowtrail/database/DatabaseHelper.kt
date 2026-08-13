package mx.utng.snowtrail.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * ARCHIVO: DatabaseHelper.kt
 * PROPÓSITO: Base de Datos y Persistencia Local Relacional (Data Layer).
 * Gestiona el esquema SQLite local (snowtrail.db) en versión 5 para soporte offline y sincronización:
 * - Tabla users: Autenticación y control de acceso por roles (CLIENTE y ADMIN).
 * - Tabla shops: Directorio geolocalizado de sucursales con coordenadas y horarios.
 * - Tabla user_favorites: Tabla puente (user_email, shop_id) para listas independientes.
 * - Tabla promotions: Ofertas y promociones indexadas por heladería.
 * - Tablas orders y order_products: Control cabecera-detalle con máquina de estados finita.
 * - Tabla notifications: Historial de alertas y avisos de proximidad con marcas de tiempo.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "snowtrail.db"
        private const val DATABASE_VERSION = 5

        // Tablas del esquema relacional
        const val TABLE_USERS = "users"
        const val TABLE_SHOPS = "shops"
        const val TABLE_ORDERS = "orders"
        const val TABLE_ORDER_PRODUCTS = "order_products"
        const val TABLE_NOTIFICATIONS = "notifications"
        const val TABLE_PROMOTIONS = "promotions"

        // Columnas de autenticación y roles de usuario
        const val USER_EMAIL = "email"
        const val USER_PASSWORD = "password"
        const val USER_ROLE = "role"


        // Shops columns
        const val SHOP_ID = "id"
        const val SHOP_NAME = "nombre"
        const val SHOP_DISTANCE = "distancia"
        const val SHOP_FAVORITE = "es_favorita"
        const val SHOP_PROMOTION = "tiene_promocion"
        const val SHOP_HORARIO = "horario"
        const val SHOP_CONTACTO = "contacto"
        const val SHOP_DIRECCION = "direccion"

        // Promotions columns
        const val PROMO_ID = "id"
        const val PROMO_NAME = "nombre"
        const val PROMO_START = "fecha_inicio"
        const val PROMO_END = "fecha_fin"
        const val PROMO_NOTE = "nota"

        // Orders columns
        const val ORDER_ID = "id"
        const val ORDER_SHOP_ID = "neveria_id"
        const val ORDER_SHOP_NAME = "neveria_nombre"
        const val ORDER_STATUS = "estado"
        const val ORDER_ETA = "tiempo_estimado_minutos"
        const val ORDER_TIMESTAMP = "fecha_hora_millis"
        const val ORDER_TOTAL = "total"

        // Order Products columns
        const val PROD_ID = "id"
        const val PROD_ORDER_ID = "pedido_id"
        const val PROD_NAME = "nombre"
        const val PROD_QTY = "cantidad"
        const val PROD_PRICE = "precio_unitario"

        // Notifications columns
        const val NOTIF_ID = "id"
        const val NOTIF_MESSAGE = "mensaje"
        const val NOTIF_TYPE = "tipo"
        const val NOTIF_READ = "leida"
        const val NOTIF_TIMESTAMP = "fecha_envio"

        const val TABLE_USER_FAVORITES = "user_favorites"
        const val UF_USER_EMAIL = "user_email"
        const val UF_SHOP_ID = "shop_id"
    }

    override fun onCreate(db: SQLiteDatabase) {
        val createShopsTable = """
            CREATE TABLE $TABLE_SHOPS (
                $SHOP_ID TEXT PRIMARY KEY,
                $SHOP_NAME TEXT,
                $SHOP_DISTANCE REAL,
                $SHOP_FAVORITE INTEGER DEFAULT 0,
                $SHOP_PROMOTION INTEGER DEFAULT 0,
                $SHOP_HORARIO TEXT,
                $SHOP_CONTACTO TEXT,
                $SHOP_DIRECCION TEXT
            )
        """.trimIndent()

        val createOrdersTable = """
            CREATE TABLE $TABLE_ORDERS (
                $ORDER_ID TEXT PRIMARY KEY,
                $ORDER_SHOP_ID TEXT,
                $ORDER_SHOP_NAME TEXT,
                $ORDER_STATUS TEXT,
                $ORDER_ETA INTEGER,
                $ORDER_TIMESTAMP INTEGER,
                $ORDER_TOTAL REAL,
                user_email TEXT DEFAULT 'Cliente@gmail.com'
            )
        """.trimIndent()

        val createOrderProductsTable = """
            CREATE TABLE $TABLE_ORDER_PRODUCTS (
                $PROD_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $PROD_ORDER_ID TEXT,
                $PROD_NAME TEXT,
                $PROD_QTY INTEGER,
                $PROD_PRICE REAL,
                FOREIGN KEY($PROD_ORDER_ID) REFERENCES $TABLE_ORDERS($ORDER_ID) ON DELETE CASCADE
            )
        """.trimIndent()

        val createNotificationsTable = """
            CREATE TABLE $TABLE_NOTIFICATIONS (
                $NOTIF_ID TEXT PRIMARY KEY,
                $NOTIF_MESSAGE TEXT,
                $NOTIF_TYPE TEXT,
                $NOTIF_READ INTEGER DEFAULT 0,
                $NOTIF_TIMESTAMP INTEGER
            )
        """.trimIndent()

        val createUsersTable = """
            CREATE TABLE $TABLE_USERS (
                $USER_EMAIL TEXT PRIMARY KEY,
                $USER_PASSWORD TEXT,
                $USER_ROLE TEXT
            )
        """.trimIndent()

        val createPromotionsTable = """
            CREATE TABLE $TABLE_PROMOTIONS (
                $PROMO_ID TEXT PRIMARY KEY,
                $PROMO_NAME TEXT,
                $PROMO_START TEXT,
                $PROMO_END TEXT,
                $PROMO_NOTE TEXT
            )
        """.trimIndent()

        val createUserFavoritesTable = """
            CREATE TABLE IF NOT EXISTS $TABLE_USER_FAVORITES (
                $UF_USER_EMAIL TEXT,
                $UF_SHOP_ID TEXT,
                PRIMARY KEY ($UF_USER_EMAIL, $UF_SHOP_ID)
            )
        """.trimIndent()

        db.execSQL(createShopsTable)
        db.execSQL(createOrdersTable)
        db.execSQL(createOrderProductsTable)
        db.execSQL(createNotificationsTable)
        db.execSQL(createUsersTable)
        db.execSQL(createPromotionsTable)
        db.execSQL(createUserFavoritesTable)

        // Seed Admin user
        val seedAdmin = "INSERT INTO $TABLE_USERS ($USER_EMAIL, $USER_PASSWORD, $USER_ROLE) VALUES ('Admin@gmail.com', 'admin123', 'ADMIN')"
        db.execSQL(seedAdmin)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ORDER_PRODUCTS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ORDERS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_SHOPS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NOTIFICATIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROMOTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_USER_FAVORITES")
        onCreate(db)
    }
}
