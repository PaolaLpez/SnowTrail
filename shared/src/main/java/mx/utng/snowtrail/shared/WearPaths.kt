package mx.utng.snowtrail.shared

/**
 * ARCHIVO: WearPaths.kt
 * PROPÓSITO: Protocolo de comunicación y rutas del Data Layer (Google Play Services Wearable API).
 * Define los endpoints estáticos para sincronización de estados persistentes (DataClient) y despacho de eventos bidireccionales en tiempo real (MessageClient).
 */
object WearPaths {
    // ==========================================
    // RUTAS DE ESTADO PERSISTENTE (DataClient)
    // Sincronización automática de estado entre dispositivos
    // ==========================================
    
    /** Ruta DataClient para sincronizar la estructura del pedido activo (MockOrder / PedidoResumen). */
    const val PATH_PEDIDO_ACTIVO = "/pedido_activo"

    /** Ruta DataClient para sincronizar la lista de neverías cercanas geolocalizadas. */
    const val PATH_NEVERIAS_CERCANAS = "/neverias_cercanas"

    /** Ruta DataClient para sincronizar la bandeja de notificaciones y cupones. */
    const val PATH_NOTIFICACIONES = "/notificaciones"

    // ==========================================
    // COMANDOS Y EVENTOS (MessageClient)
    // Máquina de estados de pedidos y acciones de usuario
    // ==========================================

    /** Comando MessageClient enviado desde el reloj para cambiar estado de pedido a ACEPTADO. */
    const val MSG_ACEPTAR_PEDIDO = "/accion/aceptar"

    /** Comando MessageClient enviado desde el reloj para cambiar estado de pedido a ENTREGADO. */
    const val MSG_ENTREGAR_PEDIDO = "/accion/entregar"

    /** Comando MessageClient enviado desde el reloj para cambiar estado de pedido a POSPONER. */
    const val MSG_POSPONER_PEDIDO = "/accion/posponer"

    /** Comando MessageClient enviado desde el reloj para cambiar estado de pedido a RECHAZAR. */
    const val MSG_RECHAZAR_PEDIDO = "/accion/rechazar"
    
    // Gestión de catálogo y preferencias de usuario

    /** Evento MessageClient para marcar o desmarcar una heladería como favorita. */
    const val MSG_TOGGLE_FAVORITO = "/accion/toggle_favorito"

    /** Evento MessageClient para abrir la pantalla de detalle de una nevería específica. */
    const val MSG_ABRIR_DETALLE_NEVERIA = "/accion/abrir_detalle"
    
    // Gestión de alertas y notificaciones push

    /** Evento MessageClient para abrir la notificación o cupón en el smartphone. */
    const val MSG_ABRIR_NOTIFICACION = "/accion/abrir_notificacion"

    /** Evento MessageClient para descartar una notificación en la bandeja. */
    const val MSG_DESCARTAR_NOTIFICACION = "/accion/descartar_notificacion"

    // Avisos de proximidad geográfica (GPS Geofencing / Trigger)

    /** Ruta de evento prioritario de alerta de proximidad (<100m) disparado por el smartphone. */
    const val PATH_ALARMA_PROXIMIDAD = "/alarma_proximidad"

    // Monitoreo de conectividad y latencia (Heartbeat)

    /** Ruta de verificación de enlace o latencia activa entre smartphone y smartwatch. */
    const val PATH_HEARTBEAT = "/heartbeat"
}

