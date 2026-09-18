package com.turnos.servicio;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

public class EstadoPublico {

    private static final EstadoPublico INSTANCIA = new EstadoPublico();

    private final StringProperty turnoLlamado = new SimpleStringProperty("---");
    private final StringProperty ventanillaLlamada = new SimpleStringProperty("-");
    private final ObservableList<String> historial = FXCollections.observableArrayList();

    private EstadoPublico() {
    }

    public static EstadoPublico getInstancia() {
        return INSTANCIA;
    }

    public void llamarTurno(String turno, String ventanilla) {
        turnoLlamado.set(turno);
        ventanillaLlamada.set(ventanilla);
        historial.add(0, turno + "  →  Ventanilla " + ventanilla);
    }

    public StringProperty turnoLlamadoProperty() {
        return turnoLlamado;
    }

    public StringProperty ventanillaLlamadaProperty() {
        return ventanillaLlamada;
    }

    public ObservableList<String> getHistorial() {
        return historial;
    }
}
