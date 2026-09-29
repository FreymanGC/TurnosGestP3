package com.turnos.modelo;

public record Servicio(int id, String prefijo, String nombre, boolean activo) {

    @Override
    public String toString() {
        return nombre;
    }
}
