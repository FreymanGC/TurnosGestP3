package com.turnos.servicio;

import com.turnos.dao.UsuarioDAO;
import com.turnos.excepcion.ReglaNegocioException;
import com.turnos.modelo.Rol;
import com.turnos.modelo.Usuario;
import com.turnos.util.HashClave;

import java.util.List;

/** Administración de usuarios (administradores y funcionarios/operadores). */
public class UsuarioService {

    private static final String FORMATO_USUARIO = "[A-Za-z0-9._]{3,40}";
    private static final int LARGO_MINIMO_CLAVE = 4;

    private final UsuarioDAO usuarioDAO;

    public UsuarioService() {
        this(new UsuarioDAO());
    }

    public UsuarioService(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = usuarioDAO;
    }

    /** Lista sin los hash de las claves (es lo que viaja al administrador). */
    public List<Usuario> listar() {
        return usuarioDAO.listar().stream().map(Usuario::sinClave).toList();
    }

    /**
     * Crea (id = 0) o actualiza un usuario. Al editar, una clave vacía conserva la anterior.
     * El administrador no puede desactivarse ni quitarse su propio rol.
     */
    public Usuario guardar(Usuario usuario, String clave, Usuario solicitante) {
        boolean esNuevo = usuario.id() == 0;
        String nombreUsuario = usuario.nombreUsuario() == null ? "" : usuario.nombreUsuario().trim();
        String nombreCompleto = usuario.nombreCompleto() == null ? "" : usuario.nombreCompleto().trim();
        boolean cambiaClave = clave != null && !clave.isEmpty();

        if (!nombreUsuario.matches(FORMATO_USUARIO)) {
            throw new ReglaNegocioException("El usuario debe tener de 3 a 40 caracteres: letras, números, punto o guion bajo.");
        }
        if (nombreCompleto.isEmpty() || nombreCompleto.length() > 120) {
            throw new ReglaNegocioException("Digite el nombre completo (máximo 120 caracteres).");
        }
        if (usuario.rol() == null) {
            throw new ReglaNegocioException("Seleccione el rol del usuario.");
        }
        if (esNuevo && !cambiaClave) {
            throw new ReglaNegocioException("Digite una contraseña para el usuario nuevo.");
        }
        if (cambiaClave && clave.length() < LARGO_MINIMO_CLAVE) {
            throw new ReglaNegocioException("La contraseña debe tener al menos " + LARGO_MINIMO_CLAVE + " caracteres.");
        }
        if (usuarioDAO.existeNombreUsuario(nombreUsuario, usuario.id())) {
            throw new ReglaNegocioException("Ya existe un usuario llamado \"" + nombreUsuario + "\".");
        }
        if (solicitante != null && solicitante.id() == usuario.id() && (!usuario.activo() || usuario.rol() != Rol.ADMIN)) {
            throw new ReglaNegocioException("No puede desactivarse ni quitarse el rol de administrador a sí mismo.");
        }

        Usuario limpio = new Usuario(usuario.id(), nombreUsuario, null, nombreCompleto, usuario.rol(), usuario.activo());
        if (esNuevo) {
            int id = usuarioDAO.insertar(limpio, HashClave.sha256Hex(clave));
            return new Usuario(id, nombreUsuario, null, nombreCompleto, usuario.rol(), usuario.activo());
        }
        usuarioDAO.actualizar(limpio);
        if (cambiaClave) {
            usuarioDAO.actualizarClave(usuario.id(), HashClave.sha256Hex(clave));
        }
        return limpio;
    }
}
