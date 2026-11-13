package com.turnos.servidor;

import com.turnos.modelo.TurnoLlamado;
import com.turnos.protocolo.TipoMensaje;
import com.turnos.servicio.NotificadorLlamados;

/**
 * Implementación definitiva de NotificadorLlamados: en lugar de actualizar una pantalla en
 * memoria, envía el turno llamado por socket a todos los clientes conectados.
 */
class NotificadorRed implements NotificadorLlamados {

    private final CentroNotificaciones centro;

    NotificadorRed(CentroNotificaciones centro) {
        this.centro = centro;
    }

    @Override
    public void turnoLlamado(TurnoLlamado llamado) {
        centro.publicar(TipoMensaje.EVENTO_TURNO_LLAMADO, llamado);
    }

    @Override
    public void turnoRetirado(String codigo) {
        centro.publicar(TipoMensaje.EVENTO_TURNO_RETIRADO, codigo);
    }
}