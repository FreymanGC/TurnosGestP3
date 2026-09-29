package com.turnos.excepcion;

/** Base de los errores cuyo mensaje es seguro y comprensible para mostrar al usuario. */
public class TurnosException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TurnosException(String mensajeParaUsuario) {
        super(mensajeParaUsuario);
    }

    public TurnosException(String mensajeParaUsuario, Throwable causa) {
        super(mensajeParaUsuario, causa);
    }
}
