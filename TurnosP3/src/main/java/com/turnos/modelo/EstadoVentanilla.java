package com.turnos.modelo;

import java.io.Serializable;

public record EstadoVentanilla(Ventanilla ventanilla, String operadorConectado, String turnoActual)
        implements Serializable {
}
