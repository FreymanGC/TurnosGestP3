package com.turnos.cliente;

import com.turnos.protocolo.Mensaje;


public interface ObservadorServidor {

    void alRecibirEvento(Mensaje evento);

    default void alPerderConexion() {
    }
}
