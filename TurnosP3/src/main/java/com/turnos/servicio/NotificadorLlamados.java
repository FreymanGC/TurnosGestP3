package com.turnos.servicio;

import com.turnos.modelo.TurnoLlamado;

/**
 * Punto de extensión para avisar a las pantallas públicas que se llamó un turno.
 * Hoy la única implementación es NotificadorLocal (memoria, mismo proceso). La implementación
 * definitiva enviará el TurnoLlamado por socket al servidor central.
 */
public interface NotificadorLlamados {

    void turnoLlamado(TurnoLlamado llamado);
}
