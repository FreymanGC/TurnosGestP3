package com.turnos.servidor;

import com.turnos.modelo.Usuario;
import com.turnos.modelo.Ventanilla;

/** Quién está conectado en un socket: el usuario que inició sesión y su ventanilla (si tiene). */
class SesionCliente {

    private volatile Usuario usuario;
    private volatile Ventanilla ventanilla;

    Usuario getUsuario() {
        return usuario;
    }

    Ventanilla getVentanilla() {
        return ventanilla;
    }

    void iniciar(Usuario usuario) {
        this.usuario = usuario;
        this.ventanilla = null;
    }

    void asignarVentanilla(Ventanilla ventanilla) {
        this.ventanilla = ventanilla;
    }

    void cerrar() {
        usuario = null;
        ventanilla = null;
    }

    String descripcion() {
        if (usuario == null) {
            return "cliente sin sesión";
        }
        return usuario.nombreUsuario() + (ventanilla != null ? " (" + ventanilla.etiqueta() + ")" : "");
    }
}
