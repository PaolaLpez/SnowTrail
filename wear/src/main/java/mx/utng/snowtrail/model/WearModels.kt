package mx.utng.snowtrail.model

/**
 * ARCHIVO: WearModels.kt
 * PROPÓSITO: Modelos de datos inmutables y livianos optimizados para la capa de sincronización Wearable Data Layer.
 */

/**
 * Modelo de Resumen de Pedido para la pantalla de reloj.
 */
data class PedidoResumen(
    val id: String = "",
    val neveriaId: String = "",
    val neveriaNombre: String = "",
    val estado: String = "NUEVO", // NUEVO, ACEPTADO, POSPUESTO, RECHAZADO, ENTREGADO
    val tiempoEstimadoMinutos: Long = 0,
    val fechaHoraMillis: Long = 0,
    val total: Double = 0.0,
    val productos: List<ProductoResumen> = emptyList()
)

/**
 * Modelo de Resumen de Producto individual dentro de una orden.
 */
data class ProductoResumen(
    val nombre: String = "",
    val cantidad: Int = 0,
    val precioUnitario: Double = 0.0
)

/**
 * Modelo de Resumen de Nevería para el directorio geolocalizado del smartwatch.
 */
data class NeveriaResumen(
    val id: String = "",
    val nombre: String = "",
    val distancia: Double = 0.0,
    val esFavorita: Boolean = false,
    val tienePromocion: Boolean = false
)

/**
 * Modelo de Resumen de Notificación para la bandeja de avisos del reloj.
 */
data class NotificacionResumen(
    val id: String = "",
    val mensaje: String = "",
    val tipo: String = "CAMBIO_ESTADO", // CAMBIO_ESTADO, PROMOCION, PROXIMIDAD
    val leida: Boolean = false,
    val fechaEnvio: Long = 0
)

/**
 * Modelo de Alerta de Proximidad geolocalizada para el diálogo emergente del smartwatch.
 */
data class ProximityAlert(
    val shopName: String,
    val distanceMeters: Int,
    val promoNote: String
)
