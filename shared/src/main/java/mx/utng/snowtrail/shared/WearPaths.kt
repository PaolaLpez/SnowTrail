package mx.utng.snowtrail.shared

/**
 * Protocolo de comunicación y rutas del Data Layer (Google Play Services Wearable API).
 * Define los endpoints para sincronización de estados persistentes (DataClient)
 * y despacho de eventos/acciones bidireccionales en tiempo real (MessageClient).
 */
object WearPaths {
    // ==========================================
    // RUTAS DE ESTADO PERSISTENTE (DataClient)
    // Sincronización automática de estado entre dispositivos
    // ==========================================
    const val PATH_PEDIDO_ACTIVO = "/pedido_activo"
    const val PATH_NEVERIAS_CERCANAS = "/neverias_cercanas"
    const val PATH_NOTIFICACIONES = "/notificaciones"

    // ==========================================
    // COMANDOS Y EVENTOS (MessageClient)
    // Máquina de estados de pedidos y acciones de usuario
    // ==========================================
    const val MSG_ACEPTAR_PEDIDO = "/accion/aceptar"
    const val MSG_ENTREGAR_PEDIDO = "/accion/entregar"
    const val MSG_POSPONER_PEDIDO = "/accion/posponer"
    const val MSG_RECHAZAR_PEDIDO = "/accion/rechazar"
    
    // Gestión de catálogo y preferencias de usuario
    const val MSG_TOGGLE_FAVORITO = "/accion/toggle_favorito"
    const val MSG_ABRIR_DETALLE_NEVERIA = "/accion/abrir_detalle"
    
    // Gestión de alertas y notificaciones push
    const val MSG_ABRIR_NOTIFICACION = "/accion/abrir_notificacion"
    const val MSG_DESCARTAR_NOTIFICACION = "/accion/descartar_notificacion"

    // Avisos de proximidad geográfica (GPS Geofencing / Trigger)
    const val PATH_ALARMA_PROXIMIDAD = "/alarma_proximidad"

    // Monitoreo de conectividad y latencia (Heartbeat)
    const val PATH_HEARTBEAT = "/heartbeat"
}

