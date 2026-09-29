package com.turnos.servicio;

import com.turnos.modelo.TurnoLlamado;

/** SOLUCIÓN TEMPORAL DE DESARROLLO: publica en EstadoPublico (memoria del mismo proceso). */
public class NotificadorLocal implements NotificadorLlamados {

    @Override
    public void turnoLlamado(TurnoLlamado llamado) {
        EstadoPublico.getInstancia().publicar(llamado);
    }
}
