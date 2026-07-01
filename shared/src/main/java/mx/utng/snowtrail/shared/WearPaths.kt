package mx.utng.snowtrail.shared

object WearPaths {
    // Data Layer DataItem paths (Synchronized persistent states)
    const val PATH_PEDIDO_ACTIVO = "/pedido_activo"
    const val PATH_NEVERIAS_CERCANAS = "/neverias_cercanas"
    const val PATH_NOTIFICACIONES = "/notificaciones"

    // Data Layer Message paths (Events / Actions)
    const val MSG_ACEPTAR_PEDIDO = "/accion/aceptar"
    const val MSG_ENTREGAR_PEDIDO = "/accion/entregar"
    const val MSG_POSPONER_PEDIDO = "/accion/posponer"
    const val MSG_RECHAZAR_PEDIDO = "/accion/rechazar"
    
    // Command payload is expected to contain the shop ID
    const val MSG_TOGGLE_FAVORITO = "/accion/toggle_favorito"
    
    // Command payload contains the shop ID to open on mobile
    const val MSG_ABRIR_DETALLE_NEVERIA = "/accion/abrir_detalle"
    
    // Command payload contains notification ID
    const val MSG_ABRIR_NOTIFICACION = "/accion/abrir_notificacion"
    const val MSG_DESCARTAR_NOTIFICACION = "/accion/descartar_notificacion"

    // Proximity alert event path (Sent from mobile to Wear OS, payload is shop info)
    const val PATH_ALARMA_PROXIMIDAD = "/alarma_proximidad"

    // Heartbeat ping
    const val PATH_HEARTBEAT = "/heartbeat"
}
