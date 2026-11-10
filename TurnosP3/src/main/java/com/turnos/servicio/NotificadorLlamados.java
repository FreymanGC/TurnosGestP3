package com.turnos.servicio;

import com.turnos.modelo.TurnoLlamado;

/**
 * Punto de extensión para avisar a las pantallas públicas que se llamó un turno.
 * TurnoService no sabe CÓMO se avisa; la implementación del servidor (NotificadorRed) envía
 * el TurnoLlamado por socket a todos los clientes conectados.
 */
public interface NotificadorLlamados {

    void turnoLlamado(TurnoLlamado llamado);
}
