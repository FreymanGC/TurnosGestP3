package com.turnos.modelo;

/**
 * Refleja la tabla estado_turno. Los ids coinciden con los datos sembrados en la BD
 * (1 EN_ESPERA ... 5 CANCELADO); si el catálogo cambia, este enum debe cambiar con él.
 */
public enum EstadoTurno {
    EN_ESPERA(1, "En espera", false),
    LLAMADO(2, "Llamado", false),
    EN_ATENCION(3, "En atención", false),
    FINALIZADO(4, "Finalizado", true),
    CANCELADO(5, "Cancelado", true);

    private final int id;
    private final String nombre;
    private final boolean esFinal;

    EstadoTurno(int id, String nombre, boolean esFinal) {
        this.id = id;
        this.nombre = nombre;
        this.esFinal = esFinal;
    }

    public int getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public boolean esFinal() {
        return esFinal;
    }

    public static EstadoTurno desdeId(int id) {
        for (EstadoTurno estado : values()) {
            if (estado.id == id) {
                return estado;
            }
        }
        throw new IllegalArgumentException("Estado de turno desconocido: " + id);
    }
}
