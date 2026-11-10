package com.turnos.protocolo;

/**
 * Tipos de mensaje que viajan por el socket.
 * Solicitudes: las envía un cliente; el servidor contesta con RESPUESTA (mismo id).
 * EVENTO_*: el servidor los envía por su cuenta a todos los clientes (patrón Observer).
 */
public enum TipoMensaje {

    RESPUESTA,

    // Públicas (generadora y pantalla pública, sin sesión)
    PING,
    LISTAR_SERVICIOS_ACTIVOS,
    GENERAR_TURNO,
    ULTIMOS_LLAMADOS,
    URL_CONSULTA_TURNO,

    // Sesión
    LOGIN,
    LOGOUT,

    // Operador
    LISTAR_VENTANILLAS,
    SELECCIONAR_VENTANILLA,
    TURNO_ACTIVO,
    HISTORIAL_VENTANILLA,
    LLAMAR_SIGUIENTE,
    VOLVER_A_LLAMAR,
    INICIAR_ATENCION,
    FINALIZAR_ATENCION,
    MARCAR_AUSENTE,
    CAMBIAR_DISPONIBILIDAD,

    // Administrador
    USUARIOS_LISTAR,
    USUARIO_GUARDAR,
    SERVICIOS_LISTAR_TODOS,
    SERVICIO_GUARDAR,
    VENTANILLAS_ESTADO,
    VENTANILLA_GUARDAR,
    VENTANILLA_SERVICIOS,
    VENTANILLA_SERVICIOS_GUARDAR,
    REPORTE,

    // Eventos del servidor
    EVENTO_TURNO_LLAMADO,
    EVENTO_SERVICIOS_ACTUALIZADOS,
    EVENTO_VENTANILLAS_ACTUALIZADAS;

    public boolean esEvento() {
        return name().startsWith("EVENTO_");
    }
}