package com.turnos.modelo;

import java.io.Serializable;
import java.time.LocalDateTime;

/** Dato mínimo que necesita la pantalla pública; es el aviso que el servidor envía por el socket. */
public record TurnoLlamado(String codigo, int numeroVentanilla, LocalDateTime fechaHoraLlamado) implements Serializable {
}
