package com.turnos.protocolo;


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
    /** Un turno terminó (finalizado o no se presentó): la pantalla pública lo quita. */
    EVENTO_TURNO_RETIRADO,
    EVENTO_SERVICIOS_ACTUALIZADOS,
    EVENTO_VENTANILLAS_ACTUALIZADAS;

    public boolean esEvento() {
        return name().startsWith("EVENTO_");
    }
}