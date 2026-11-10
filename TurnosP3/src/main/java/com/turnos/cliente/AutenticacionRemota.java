package com.turnos.cliente;

import com.turnos.modelo.Rol;
import com.turnos.modelo.Usuario;
import com.turnos.protocolo.DatosLogin;
import com.turnos.protocolo.TipoMensaje;


public class AutenticacionRemota {

    private final ClienteRed cliente = ClienteRed.getInstancia();

    public Usuario autenticar(String nombreUsuario, String clave, Rol rolRequerido) {
        return cliente.enviar(TipoMensaje.LOGIN, new DatosLogin(nombreUsuario, clave, rolRequerido));
    }

    public void cerrarSesion() {
        if (cliente.estaConectado()) {
            cliente.enviar(TipoMensaje.LOGOUT, null);
        }
    }
}
