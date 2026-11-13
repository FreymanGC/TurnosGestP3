package com.turnos.cliente;

import com.turnos.modelo.TurnoLlamado;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;


public class EstadoPublico {

    private static final int MAXIMO_HISTORIAL = 10;
    private static final EstadoPublico INSTANCIA = new EstadoPublico();

    private final StringProperty turnoLlamado = new SimpleStringProperty("---");
    private final StringProperty ventanillaLlamada = new SimpleStringProperty("-");
    private final ObservableList<TurnoLlamado> historial = FXCollections.observableArrayList();

    private EstadoPublico() {
    }

    public static EstadoPublico getInstancia() {
        return INSTANCIA;
    }

    public void publicar(TurnoLlamado llamado) {
        enHiloFx(() -> {
            turnoLlamado.set(llamado.codigo());
            ventanillaLlamada.set(String.valueOf(llamado.numeroVentanilla()));
            // si vuelven a llamar el mismo turno, se quita la fila vieja: cada turno sale una sola vez
            historial.removeIf(t -> t.codigo().equals(llamado.codigo()));
            historial.add(0, llamado);
            while (historial.size() > MAXIMO_HISTORIAL) {
                historial.remove(historial.size() - 1);
            }
        });
    }

    /** Quita de la pantalla un turno que ya terminó (finalizado o no se presentó). */
    public void retirar(String codigo) {
        enHiloFx(() -> {
            historial.removeIf(t -> t.codigo().equals(codigo));
            if (historial.isEmpty()) {
                turnoLlamado.set("---");
                ventanillaLlamada.set("-");
            } else {
                TurnoLlamado primero = historial.get(0);
                turnoLlamado.set(primero.codigo());
                ventanillaLlamada.set(String.valueOf(primero.numeroVentanilla()));
            }
        });
    }

    /** Recibe los llamados del más reciente al más antiguo. */
    public void cargarHistorial(List<TurnoLlamado> llamados) {
        enHiloFx(() -> {
            if (llamados.isEmpty()) {
                historial.clear();
                turnoLlamado.set("---");
                ventanillaLlamada.set("-");
                return;
            }
            TurnoLlamado ultimo = llamados.get(0);
            turnoLlamado.set(ultimo.codigo());
            ventanillaLlamada.set(String.valueOf(ultimo.numeroVentanilla()));
            historial.setAll(llamados.subList(0, Math.min(MAXIMO_HISTORIAL, llamados.size())));
        });
    }

    public StringProperty turnoLlamadoProperty() {
        return turnoLlamado;
    }

    public StringProperty ventanillaLlamadaProperty() {
        return ventanillaLlamada;
    }

    public ObservableList<TurnoLlamado> getHistorial() {
        return historial;
    }

    private static void enHiloFx(Runnable accion) {
        if (Platform.isFxApplicationThread()) {
            accion.run();
        } else {
            Platform.runLater(accion);
        }
    }
}