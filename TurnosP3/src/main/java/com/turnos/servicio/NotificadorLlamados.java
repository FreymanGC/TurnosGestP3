package com.turnos.servicio;

import com.turnos.modelo.TurnoLlamado;


public interface NotificadorLlamados {

    void turnoLlamado(TurnoLlamado llamado);

    /** Avisa que el turno con este código ya terminó (finalizado o ausente) y debe salir de la pantalla. */
    default void turnoRetirado(String codigo) {
    }
}