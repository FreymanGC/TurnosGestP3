package com.turnos.servicio;

import com.turnos.dao.UsuarioDAO;
import com.turnos.excepcion.ReglaNegocioException;
import com.turnos.modelo.Rol;
import com.turnos.modelo.Usuario;
import com.turnos.util.HashClave;

public class AutenticacionService {

    private static final String MSG_CAMPOS_VACIOS = "Debe ingresar usuario y contraseña.";
    private static final String MSG_CREDENCIALES = "Usuario o contraseña incorrectos.";
    private static final String MSG_INACTIVO = "Su usuario está inactivo. Contacte al administrador.";
    private static final String MSG_SIN_PERMISO = "Su usuario no tiene permiso para usar esta aplicación.";

    private final UsuarioDAO usuarioDAO;

    public AutenticacionService() {
        this(new UsuarioDAO());
    }

    public AutenticacionService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /**
     * Usuario inexistente y contraseña errónea dan el mismo mensaje a propósito, para no revelar
     * qué usuarios existen. El estado inactivo se informa solo cuando la contraseña es correcta.
     */
    public Usuario autenticar(String nombreUsuario, String clave, Rol rolRequerido) {
        if (nombreUsuario == null || nombreUsuario.isBlank() || clave == null || clave.isEmpty()) {
            throw new ReglaNegocioException(MSG_CAMPOS_VACIOS);
        }
        Usuario usuario = usuarioDAO.buscarPorNombreUsuario(nombreUsuario.trim())
                .filter(u -> HashClave.coincide(clave, u.claveHash()))
                .orElseThrow(() -> new ReglaNegocioException(MSG_CREDENCIALES));

        if (!usuario.activo()) {
            throw new ReglaNegocioException(MSG_INACTIVO);
        }
        if (rolRequerido != null && usuario.rol() != rolRequerido) {
            throw new ReglaNegocioException(MSG_SIN_PERMISO);
        }
        return usuario;
    }
}
