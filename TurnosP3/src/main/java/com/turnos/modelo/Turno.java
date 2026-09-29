package com.turnos.modelo;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record Turno(
        long id,
        int idServicio,
        LocalDate fechaOperacion,
        int numero,
        String codigo,
        EstadoTurno estado,
        LocalDateTime fechaHoraGeneracion,
        LocalDateTime fechaHoraLlamado,
        LocalDateTime fechaHoraInicioAtencion,
        LocalDateTime fechaHoraFinalizacion,
        Integer idVentanilla,
        Integer idUsuario) {
}
