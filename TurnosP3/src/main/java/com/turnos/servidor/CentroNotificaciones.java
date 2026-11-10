package com.turnos.servidor;

import com.turnos.protocolo.Mensaje;
import com.turnos.protocolo.TipoMensaje;
import java.io.Serializable;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Patrón Observer (sujeto). Cada cliente conectado (ManejadorCliente) se suscribe; cuando ocurre
 * algo (por ejemplo, se llamó un turno) el servidor publica el evento y se reenvía a todos.
 * CopyOnWriteArrayList: se puede recorrer en un hilo mientras otro suscribe o desuscribe.
 */
class CentroNotificaciones {

    private final CopyOnWriteArrayList<ObservadorEventos> suscriptores = new CopyOnWriteArrayList<>();

    void suscribir(ObservadorEventos observador) {
        suscriptores.addIfAbsent(observador);
    }

    void desuscribir(ObservadorEventos observador) {
        suscriptores.remove(observador);
    }

    void publicar(TipoMensaje tipo, Serializable datos) {
        Mensaje evento = Mensaje.evento(tipo, datos);
        for (ObservadorEventos observador : suscriptores) {
            observador.notificar(evento);
        }
    }
}
