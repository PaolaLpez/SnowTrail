package mx.utng.snowtrail.model

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

data class ProductoResumen(
    val nombre: String = "",
    val cantidad: Int = 0,
    val precioUnitario: Double = 0.0
)

data class NeveriaResumen(
    val id: String = "",
    val nombre: String = "",
    val distancia: Double = 0.0,
    val esFavorita: Boolean = false,
    val tienePromocion: Boolean = false
)

data class NotificacionResumen(
    val id: String = "",
    val mensaje: String = "",
    val tipo: String = "CAMBIO_ESTADO", // CAMBIO_ESTADO, PROMOCION, PROXIMIDAD
    val leida: Boolean = false,
    val fechaEnvio: Long = 0
)

data class ProximityAlert(
    val shopName: String,
    val distanceMeters: Int,
    val promoNote: String
)
