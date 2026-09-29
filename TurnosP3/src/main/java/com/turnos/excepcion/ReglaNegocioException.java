package com.turnos.excepcion;

/** Validación o regla incumplida (campo vacío, ventanilla no disponible, sin turnos, etc.). */
public class ReglaNegocioException extends TurnosException {

    private static final long serialVersionUID = 1L;

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
