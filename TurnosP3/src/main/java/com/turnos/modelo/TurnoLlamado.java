package com.turnos.modelo;

import java.time.LocalDateTime;

/** Dato mínimo que necesita la pantalla pública; será también el mensaje que viaje por el socket. */
public record TurnoLlamado(String codigo, int numeroVentanilla, LocalDateTime fechaHoraLlamado) {
}
