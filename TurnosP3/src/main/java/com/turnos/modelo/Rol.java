package com.turnos.modelo;

public enum Rol {
    ADMIN("Administrador"),
    OPERADOR("Operador");

    private final String nombre;

    Rol(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public static Rol desdeCodigo(String codigo) {
        return Rol.valueOf(codigo.trim().toUpperCase());
    }
}
