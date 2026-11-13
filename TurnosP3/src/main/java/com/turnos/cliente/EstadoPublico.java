package com.turnos.cliente;

import com.turnos.modelo.TurnoLlamado;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.util.List;

/**
 * Estado que muestra la Pantalla Pública (Singleton con propiedades observables de JavaFX).
 * Lo alimentan los eventos EVENTO_TURNO_LLAMADO que el servidor envía por el socket
 * (ver PantallaPublicaController). El historial guarda los llamados completos para que la
 * tabla de la pantalla pueda dibujar cada columna (ventanilla, turno y hora) por separado.
 */
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
            historial.add(0, llamado);
            while (historial.size() > MAXIMO_HISTORIAL) {
                historial.remove(historial.size() - 1);
            }
        });
    }

    /** Recibe los llamados del más reciente al más antiguo. */
    public void cargarHistorial(List<TurnoLlamado> llamados) {
        if (llamados.isEmpty()) {
            return;
        }
        enHiloFx(() -> {
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