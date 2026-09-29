package com.turnos.excepcion;

/** Falla al acceder a la BD. El mensaje ya está traducido; la causa técnica va al log. */
public class PersistenciaException extends TurnosException {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensaje, Throwable causa) {
        super(mensaje, causa);
    }
}
