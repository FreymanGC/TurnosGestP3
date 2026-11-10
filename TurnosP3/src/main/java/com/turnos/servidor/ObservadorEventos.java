package com.turnos.servidor;

import com.turnos.protocolo.Mensaje;

/** Patrón Observer (servidor): quien quiere recibir los eventos que publica el CentroNotificaciones. */
interface ObservadorEventos {

    void notificar(Mensaje evento);
}
