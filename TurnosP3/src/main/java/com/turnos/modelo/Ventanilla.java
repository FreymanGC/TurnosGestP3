package com.turnos.modelo;

import java.io.Serializable;

public record Ventanilla(int id, int numero, String nombre, boolean disponible) implements Serializable {

    public String etiqueta() {
        return (nombre == null || nombre.isBlank()) ? "Ventanilla " + numero : nombre;
    }

    @Override
    public String toString() {
        return etiqueta();
    }
}
