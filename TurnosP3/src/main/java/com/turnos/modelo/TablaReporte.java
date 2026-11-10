package com.turnos.modelo;

import java.io.Serializable;
import java.util.List;

/** Resultado de cualquiera de las 5 consultas, listo para mostrarse en una tabla. */
public record TablaReporte(String titulo, List<String> columnas, List<List<String>> filas, String resumen)
        implements Serializable {
}
