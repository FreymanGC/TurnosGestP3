package com.turnos.protocolo;

import com.turnos.modelo.Usuario;
import java.io.Serializable;

/** Usuario que el administrador quiere guardar y su clave nueva (vacía = no cambiarla). */
public record UsuarioConClave(Usuario usuario, String clave) implements Serializable {
}
