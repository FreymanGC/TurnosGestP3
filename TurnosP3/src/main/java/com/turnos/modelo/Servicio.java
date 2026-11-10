package com.turnos.modelo;

import java.io.Serializable;

/**
 * prioridad: 0 = normal (orden de llegada); mayor que 0 = preferencial, sus turnos se llaman
 * antes (innovación). Serializable porque viaja por el socket entre servidor y clientes.
 */
public record Servicio(int id, String prefijo, String nombre, int prioridad, boolean activo) implements Serializable {

    public boolean esPreferencial() {
        return prioridad > 0;
    }

    @Override
    public String toString() {
        return nombre;
    }
}
