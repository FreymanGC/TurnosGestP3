package com.turnos.modelo;

public record Usuario(int id, String nombreUsuario, String claveHash, String nombreCompleto, Rol rol, boolean activo) {

    @Override
    public String toString() {
        return "Usuario[id=" + id + ", nombreUsuario=" + nombreUsuario + ", rol=" + rol + "]";
    }
}
