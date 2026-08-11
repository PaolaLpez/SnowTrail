package mx.utng.snowtrail.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        private const val DATABASE_NAME = "snowtrail.db"
        private const val DATABASE_VERSION = 5

        // Tables
        const val TABLE_USERS = "users"
        const val TABLE_SHOPS = "shops"
        const val TABLE_ORDERS = "orders"
        const val TABLE_ORDER_PRODUCTS = "order_products"
        const val TABLE_NOTIFICATIONS = "notifications"
        const val TABLE_PROMOTIONS = "promotions"

        // Users columns
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

        db.execSQL(createShopsTable)
        db.execSQL(createOrdersTable)
        db.execSQL(createOrderProductsTable)
        db.execSQL(createNotificationsTable)
        db.execSQL(createUsersTable)
        db.execSQL(createPromotionsTable)

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
        onCreate(db)
    }
}
