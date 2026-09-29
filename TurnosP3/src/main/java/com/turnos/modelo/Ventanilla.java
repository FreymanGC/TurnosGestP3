package com.turnos.modelo;

public record Ventanilla(int id, int numero, String nombre, boolean disponible) {

    public String etiqueta() {
        return (nombre == null || nombre.isBlank()) ? "Ventanilla " + numero : nombre;
    }

    @Override
    public String toString() {
        return etiqueta();
    }
}
