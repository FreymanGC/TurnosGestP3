package com.turnos.modelo;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** Turno con los nombres de su servicio, ventanilla y funcionario, para las consultas históricas. */
public record TurnoDetallado(
        String codigo,
        String servicio,
        LocalDate fechaOperacion,
        EstadoTurno estado,
        LocalDateTime fechaHoraGeneracion,
        LocalDateTime fechaHoraLlamado,
        LocalDateTime fechaHoraInicioAtencion,
        LocalDateTime fechaHoraFinalizacion,
        Integer numeroVentanilla,
        String funcionario) implements Serializable {
}
