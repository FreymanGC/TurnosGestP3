package com.turnos.protocolo;

import com.turnos.modelo.Rol;
import java.io.Serializable;

/** Datos del formulario de login y el rol que exige la aplicación (operador o administrador). */
public record DatosLogin(String nombreUsuario, String clave, Rol rolRequerido) implements Serializable {
}
