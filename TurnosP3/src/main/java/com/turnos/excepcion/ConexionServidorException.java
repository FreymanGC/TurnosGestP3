package com.turnos.excepcion;

/** La aplicación cliente no pudo comunicarse con el servidor central. */
public class ConexionServidorException extends TurnosException {

    private static final long serialVersionUID = 1L;

    public ConexionServidorException(String mensaje) {
        super(mensaje);
    }

    public ConexionServidorException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
