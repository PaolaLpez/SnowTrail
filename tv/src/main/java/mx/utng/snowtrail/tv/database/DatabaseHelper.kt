package mx.utng.snowtrail.tv.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * ARCHIVO: DatabaseHelper.kt
 * PROPÓSITO: Ayudante de Base de Datos SQLite Local para Android TV (`snowtrail_tv.db`).
 * Administra la creación y actualización del esquema para promociones y órdenes en comandera.
 */
class DatabaseHelper(context: Context) : SQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {

    companion object {
        /** Nombre de la base de datos SQLite para la Android TV. */
        private const val DATABASE_NAME = "snowtrail_tv.db"
        
        /** Versión del esquema SQLite (Versión 7 para precarga de pedidos iniciales). */
        private const val DATABASE_VERSION = 7

        /** Nombre de la tabla de promociones. */
        const val TABLE_PROMOTIONS = "promotions"
        const val PROMO_ID = "id"
        const val PROMO_NAME = "nombre"
        const val PROMO_START = "fecha_inicio"
        const val PROMO_END = "fecha_fin"
        const val PROMO_NOTE = "nota"
        const val PROMO_IMAGE = "imagen"
        const val PROMO_SHOP_ID = "neveria_id"

        /** Nombre de la tabla de órdenes y comandera digital. */
        const val TABLE_ORDERS = "orders"
        const val ORDER_ID = "id"
        const val ORDER_CLIENT = "cliente"
        const val ORDER_PICKUP = "para_recoger"
        const val ORDER_ETA = "tiempo_entrega"
        const val ORDER_TOTAL = "total"
        const val ORDER_ITEMS = "items"
        const val ORDER_STATUS = "estado"
        const val ORDER_SHOP_ID = "neveria_id"
    }

    /**
     * Callback de creación de tablas DDL y siembra (seeding) de datos iniciales en la base de datos de TV.
     * 
     * @param db Instancia de la base de datos SQLite.
     */
    override fun onCreate(db: SQLiteDatabase) {
        try {
            // [DDL PROMOCIONES]: Definición del esquema para las tarjetas publicitarias de la marquesina
            val createPromotionsTable = """
                CREATE TABLE $TABLE_PROMOTIONS (
                    $PROMO_ID TEXT PRIMARY KEY,
                    $PROMO_NAME TEXT,
                    $PROMO_START TEXT,
                    $PROMO_END TEXT,
                    $PROMO_NOTE TEXT,
                    $PROMO_IMAGE TEXT,
                    $PROMO_SHOP_ID TEXT
                )
            """.trimIndent()

            // [DDL ÓRDENES]: Definición del esquema para la comandera digital split-screen de la cocina
            val createOrdersTable = """
                CREATE TABLE $TABLE_ORDERS (
                    $ORDER_ID TEXT PRIMARY KEY,
                    $ORDER_CLIENT TEXT,
                    $ORDER_PICKUP TEXT,
                    $ORDER_ETA TEXT,
                    $ORDER_TOTAL TEXT,
                    $ORDER_ITEMS TEXT,
                    $ORDER_STATUS TEXT,
                    $ORDER_SHOP_ID TEXT
                )
            """.trimIndent()

            // Ejecuta las sentencias SQL DDL de creación de tablas
            db.execSQL(createPromotionsTable)
            db.execSQL(createOrdersTable)

            // Invoca la siembra de datos estáticos demo para promociones y comandera
            seedPromotions(db)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Función privada para sembrar (seed) 5 promociones por cada una de las 11 neverías y pedidos de prueba.
     * 
     * @param db Instancia de SQLiteDatabase en modo escritura.
     */
    private fun seedPromotions(db: SQLiteDatabase) {
        try {
            // [SIEMBRA DEMO]: Inserciones iniciales para la marquesina publicitaria
            // 1. Los Abuelos (nev_los_abuelos)
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pa1', '2x1 en Helados de Fruta', '2026-08-01', '2026-08-30', 'Aplica en todos los sabores naturales', '🍓', 'nev_los_abuelos')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pa2', 'Copa Suprema de Fresa y Nueces', '2026-08-01', '2026-08-30', '25% de descuento los fines de semana', '🍨', 'nev_los_abuelos')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pa3', 'Nieve de Limón Gigante', '2026-08-01', '2026-08-31', 'Nieve tradicional estilo Dolores', '🍋', 'nev_los_abuelos')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pa4', 'Malteada Clásica de Vainilla', '2026-08-05', '2026-08-25', 'Lunes a jueves a mitad de precio', '🥤', 'nev_los_abuelos')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pa5', 'Paleta de Cajeta Quemada', '2026-08-01', '2026-08-20', 'Prueba la receta original de la casa', '🍦', 'nev_los_abuelos')")

            // 2. La Michoacana (nev_la_mich)
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pm1', 'Brownie Split Deluxe: \$59 MXN', '2026-08-01', '2026-08-31', 'Con bola de vainilla y fudge caliente', '🍫', 'nev_la_mich')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pm2', 'Combo 3 Paletas de Agua', '2026-08-01', '2026-08-31', 'Por solo \$35 pesos en sabores frutales', '🍧', 'nev_la_mich')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pm3', 'Litro de Helado Combinado', '2026-08-05', '2026-08-30', 'Llévate el segundo litro con 30% desc.', '🍨', 'nev_la_mich')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pm4', 'Mangonada Extrema con Chamoy', '2026-08-01', '2026-08-15', 'Con gomitas y chilito en polvo', '🥭', 'nev_la_mich')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pm5', 'Paleta de Crema Oreo', '2026-08-01', '2026-08-20', 'Paleta rellena de galleta oreo crujiente', '🍪', 'nev_la_mich')")

            // 3. Helados Bajo Cero (nev_zero)
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pz1', '¡Combo 10 Paletas x \$199!', '2026-08-01', '2026-08-30', 'Paletas de crema o agua medianas', '🍦', 'nev_zero')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pz2', 'Waffle con Nieve de Fresa', '2026-08-01', '2026-08-31', 'Bañado en chocolate belga caliente', '🧇', 'nev_zero')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pz3', 'Smoothie Loco de Mango y Piña', '2026-08-01', '2026-08-20', 'Bebida helada ultra refrescante', '🍹', 'nev_zero')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pz4', 'Helado Vegano de Coco', '2026-08-05', '2026-08-25', 'Hecho con leche de coco 100% natural', '🥥', 'nev_zero')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pz5', 'Paleta de Chicle y Malvaviscos', '2026-08-01', '2026-08-15', 'La favorita de los pequeños de la casa', '🍬', 'nev_zero')")

            // 4. Artesanales del Valle (nev_artis)
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pr1', 'Helado de Lavanda y Miel', '2026-08-01', '2026-08-31', 'Sabor exclusivo artesanal de temporada', '🍯', 'nev_artis')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pr2', 'Cono de Mezcal con Naranja', '2026-08-01', '2026-08-15', 'Solo para adultos, receta tradicional', '🍊', 'nev_artis')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pr3', 'Tarta Helado Queso-Zarzamora', '2026-08-05', '2026-08-25', 'Rebanada individual al 3x2', '🍰', 'nev_artis')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pr4', 'Nieve de Garambullo Orgánica', '2026-08-01', '2026-08-30', 'Fruto típico de la región de Dolores', '🍇', 'nev_artis')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pr5', 'Paleta Yogurt con Frutos Rojos', '2026-08-01', '2026-08-20', 'Baja en calorías y sin azúcar añadida', '🍒', 'nev_artis')")

            // 5. Heladería Lejana (nev_far)
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pf1', 'Helado de Chocolate Abuelita', '2026-08-01', '2026-08-31', 'Con un toque de canela y trozos de chocolate', '☕', 'nev_far')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pf2', 'Paleta Helada de Rompope', '2026-08-01', '2026-08-20', 'Con trocitos de nuez pecana selecta', '🥜', 'nev_far')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pf3', 'Nieve de Nopal con Piña', '2026-08-05', '2026-08-25', 'Súper fresca y digestiva, pruébala hoy', '🍍', 'nev_far')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pf4', 'Flotante de Refresco con Vainilla', '2026-08-01', '2026-08-30', 'Bebida retro con bola de nieve cremosa', '🥤', 'nev_far')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pf5', 'Sándwich de Helado Gigante', '2026-08-01', '2026-08-15', 'Galletas caseras rellenas de helado', '🥪', 'nev_far')")

            // 6. Nieves del Centenario (nev_centenario)
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pc1', 'Nieve Histórica de Tres Leches', '2026-08-01', '2026-08-31', 'Celebrando 100 años del sabor de Dolores', '🥛', 'nev_centenario')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pc2', 'Copa Centenario Tricolor', '2026-08-01', '2026-08-15', 'Nieve de limón, guanábana y fresa', '🇲🇽', 'nev_centenario')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pc3', 'Cono de Chocolate Amargo', '2026-08-05', '2026-08-25', '70% cacao mexicano orgánico del sur', '🍫', 'nev_centenario')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pc4', 'Paleta de Cajeta con Nuez', '2026-08-01', '2026-08-30', 'Tradición familiar desde hace décadas', '🌰', 'nev_centenario')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pc5', 'Nieve de Queso de Cabra con Higo', '2026-08-01', '2026-08-20', 'Sabor gourmet e inigualable en Dolores', '🧀', 'nev_centenario')")

            // 7. Gelato Italiano (nev_gelato)
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pg1', 'Gelato de Pistacho de Bronte', '2026-08-01', '2026-08-31', 'Con pistachos italianos importados y tostados', '🥑', 'nev_gelato')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pg2', 'Affogato de Expreso con Gelato', '2026-08-01', '2026-08-20', 'Café caliente servido sobre vainilla', '☕', 'nev_gelato')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pg3', 'Gelato Stracciatella Crujiente', '2026-08-05', '2026-08-25', 'Base de crema con finos hilos de chocolate', '🍦', 'nev_gelato')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pg4', 'Sorbetto de Limone di Sicilia', '2026-08-01', '2026-08-30', 'Sin lactosa, ultra fresco y natural', '🍋', 'nev_gelato')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pg5', 'Gelato de Avellana Piamonte', '2026-08-01', '2026-08-15', 'Crema italiana clásica con chocolate y avellana', '🥜', 'nev_gelato')")

            // 8. Paletería San Antonio (nev_antonio)
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pn1', 'Esquimal de Fresa con Chocolate', '2026-08-01', '2026-08-31', 'Cubierta crujiente y coco rallado de topping', '🥥', 'nev_antonio')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pn2', 'Paleta San Antonio de Kiwi', '2026-08-01', '2026-08-15', 'Kiwi natural rebanado dentro de la paleta', '🥝', 'nev_antonio')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pn3', 'Vaso Helado Vainilla con Chispas', '2026-08-05', '2026-08-25', 'Ideal para niños, llévate el segundo gratis', '🍨', 'nev_antonio')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pn4', 'Paleta Tamarindo Rellena Chamoy', '2026-08-01', '2026-08-30', 'Picosa y dulce, un antojo perfecto', '🌶️', 'nev_antonio')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pn5', 'Helado Doble Menta con Chocolate', '2026-08-01', '2026-08-20', 'Gran frescura y sabor a chocolate belga', '🍃', 'nev_antonio')")

            // 9. El Copo Dorado (nev_copo)
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pd1', 'Copo de Oro de Mango con Tajín', '2026-08-01', '2026-08-31', 'Servido en copa con chile y serpentinas', '🥭', 'nev_copo')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pd2', 'Raspado Grosella con Condensada', '2026-08-01', '2026-08-20', 'Tradicional raspado de hielo picado', '🍧', 'nev_copo')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pd3', 'Helado Plátano y Toffee', '2026-08-05', '2026-08-25', 'Con trocitos de nuez y salsa de caramelo', '🍌', 'nev_copo')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pd4', 'Paleta de Guanábana Cremosa', '2026-08-01', '2026-08-30', 'Fruta fresca de temporada seleccionada', '🍏', 'nev_copo')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pd5', 'Copa de Helado de Arándanos', '2026-08-01', '2026-08-15', 'Con crema batida y arándanos silvestres', '🍇', 'nev_copo')")

            // 10. Flor de Dolores (nev_flor)
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pl1', 'Nieve de Rosas con Almendras', '2026-08-01', '2026-08-31', 'Sabor histórico emblemático con pétalos de rosa', '🌹', 'nev_flor')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pl2', 'Helado de Flor de Naranjo', '2026-08-01', '2026-08-15', 'Sabor sutil y aromático de primavera', '🌸', 'nev_flor')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pl3', 'Nieve Tequila con Sal y Limón', '2026-08-05', '2026-08-25', 'Sabor tradicional e inigualable de la feria', '🍋', 'nev_flor')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pl4', 'Paleta de Mango, Fresa y Limón', '2026-08-01', '2026-08-30', 'Tres deliciosos colores y sabores naturales', '🍭', 'nev_flor')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('pl5', 'Helado Vainilla Papantla Premium', '2026-08-01', '2026-08-20', 'Con vaina de vainilla mexicana auténtica', '🌼', 'nev_flor')")

            // 11. HELARTE (nev_helarte) - Tradición y Calidad en Cada Cucharada
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('ph1', 'Copa Helarte Suprema (3 Bolas + Fudge)', '2026-08-01', '2026-08-31', 'Tradición y calidad en cada cucharada', '🍨', 'nev_helarte')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('ph2', 'Cono Artesanal 2x1 en Especiales', '2026-08-01', '2026-08-30', 'Vainilla, Fresa y Menta con chispitas', '🍦', 'nev_helarte')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('ph3', 'Sundae Especial de Chocolate', '2026-08-05', '2026-08-28', 'Por solo $45 MXN con salsa caliente', '🍫', 'nev_helarte')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('ph4', 'Paleta Rellena de Crema Helarte', '2026-08-01', '2026-08-25', 'Lleva 3 paletas por solo $50 pesos', '🍭', 'nev_helarte')")
            db.execSQL("INSERT INTO $TABLE_PROMOTIONS VALUES ('ph5', 'Litro Helarte Familiar 20% OFF', '2026-08-10', '2026-08-31', 'Sabores combinados a elegir para llevar', '🍧', 'nev_helarte')")

            // Seed some demo orders associated with neveria IDs
            db.execSQL("INSERT INTO $TABLE_ORDERS VALUES ('o1', 'Gerardo Manzano', 'Para recoger: 15:45 hs', '15 min', '\$120.00 MXN', '1x Vaso Chocolate\n1x Cono Vainilla', 'PENDIENTE', 'nev_los_abuelos')")
            db.execSQL("INSERT INTO $TABLE_ORDERS VALUES ('o2', 'Paola López', 'Para recoger: 16:00 hs', '10 min', '\$85.00 MXN', '2x Paletas de Limón\n1x Helado Fresa', 'PENDIENTE', 'nev_la_mich')")
            db.execSQL("INSERT INTO $TABLE_ORDERS VALUES ('o3', 'Jennifer Medina', 'Para recoger: 16:30 hs', '20 min', '\$199.00 MXN', '1x Paquete Familiar 10 Paletas', 'PENDIENTE', 'nev_zero')")
            db.execSQL("INSERT INTO $TABLE_ORDERS VALUES ('o4', 'Rodrigo Silva', 'Para recoger: 17:00 hs', '12 min', '\$59.00 MXN', '1x Brownie Split Deluxe', 'PENDIENTE', 'nev_los_abuelos')")
            db.execSQL("INSERT INTO $TABLE_ORDERS VALUES ('o5', 'Ana Paula', 'Para recoger: 17:15 hs', '10 min', '\$113.00 MXN', '1x Copa Helarte Suprema\n1x Cono Tradición', 'PENDIENTE', 'nev_helarte')")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_PROMOTIONS")
        db.execSQL("DROP TABLE IF EXISTS $TABLE_ORDERS")
        onCreate(db)
    }
}
