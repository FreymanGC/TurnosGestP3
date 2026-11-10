package com.turnos.protocolo;

import java.io.Serializable;
import java.util.List;

/** Servicios que el administrador asigna a una ventanilla. */
public record ServiciosDeVentanilla(int idVentanilla, List<Integer> idsServicios) implements Serializable {
}
