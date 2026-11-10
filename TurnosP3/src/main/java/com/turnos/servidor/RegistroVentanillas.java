package com.turnos.servidor;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;


class RegistroVentanillas {

    private final ConcurrentHashMap<Integer, SesionCliente> ocupadas = new ConcurrentHashMap<>();

    boolean ocupar(int idVentanilla, SesionCliente sesion) {
        SesionCliente actual = ocupadas.putIfAbsent(idVentanilla, sesion);
        return actual == null || actual == sesion;
    }

    void liberar(int idVentanilla, SesionCliente sesion) {
        ocupadas.remove(idVentanilla, sesion);
    }

    Optional<String> operadorEn(int idVentanilla) {
        SesionCliente sesion = ocupadas.get(idVentanilla);
        if (sesion == null || sesion.getUsuario() == null) {
            return Optional.empty();
        }
        return Optional.of(sesion.getUsuario().nombreCompleto());
    }
}
