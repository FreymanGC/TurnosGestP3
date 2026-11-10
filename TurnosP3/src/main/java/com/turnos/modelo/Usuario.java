package com.turnos.modelo;

import java.io.Serializable;

public record Usuario(int id, String nombreUsuario, String claveHash, String nombreCompleto, Rol rol, boolean activo)
        implements Serializable {

    /** Copia sin el hash de la clave: es la versión que el servidor envía a los clientes. */
    public Usuario sinClave() {
        return new Usuario(id, nombreUsuario, null, nombreCompleto, rol, activo);
    }

    @Override
    public String toString() {
        return "Usuario[id=" + id + ", nombreUsuario=" + nombreUsuario + ", rol=" + rol + "]";
    }
}
