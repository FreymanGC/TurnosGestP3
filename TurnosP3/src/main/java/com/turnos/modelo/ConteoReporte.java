package com.turnos.modelo;

import java.io.Serializable;

/** Fila de un reporte de conteo: un nombre (funcionario o servicio) y una cantidad de turnos. */
public record ConteoReporte(String nombre, long cantidad) implements Serializable {
}
