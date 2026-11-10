package com.turnos.protocolo;

import java.io.Serializable;

/**
 * Lo que viaja por el socket (ObjectOutputStream). La respuesta lleva el mismo id que la
 * solicitud, así el cliente sabe a qué pregunta corresponde. Si exito es false, texto trae el
 * mensaje de error para el usuario.
 */
public final class Mensaje implements Serializable {

    private static final long serialVersionUID = 1L;

    private final int id;
    private final TipoMensaje tipo;
    private final Serializable datos;
    private final boolean exito;
    private final String texto;

    private Mensaje(int id, TipoMensaje tipo, Serializable datos, boolean exito, String texto) {
        this.id = id;
        this.tipo = tipo;
        this.datos = datos;
        this.exito = exito;
        this.texto = texto;
    }

    public static Mensaje solicitud(int id, TipoMensaje tipo, Serializable datos) {
        return new Mensaje(id, tipo, datos, true, null);
    }

    public static Mensaje respuestaOk(int id, Serializable datos) {
        return new Mensaje(id, TipoMensaje.RESPUESTA, datos, true, null);
    }

    public static Mensaje respuestaError(int id, String texto) {
        return new Mensaje(id, TipoMensaje.RESPUESTA, null, false, texto);
    }

    public static Mensaje evento(TipoMensaje tipo, Serializable datos) {
        return new Mensaje(0, tipo, datos, true, null);
    }

    public int getId() {
        return id;
    }

    public TipoMensaje getTipo() {
        return tipo;
    }

    public Serializable getDatos() {
        return datos;
    }

    public boolean isExito() {
        return exito;
    }

    public String getTexto() {
        return texto;
    }

    @Override
    public String toString() {
        return tipo + "#" + id + (exito ? "" : " (error: " + texto + ")");
    }
}
