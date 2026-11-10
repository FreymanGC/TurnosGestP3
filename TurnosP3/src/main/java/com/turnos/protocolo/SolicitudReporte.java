package com.turnos.protocolo;

import java.io.Serializable;
import java.time.LocalDate;

/** Pide una de las 5 consultas (1 a 5) entre dos fechas. */
public record SolicitudReporte(int numeroConsulta, LocalDate desde, LocalDate hasta) implements Serializable {
}
