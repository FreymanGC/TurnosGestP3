package com.turnos.servicio;

import com.turnos.modelo.TurnoLlamado;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * SOLUCIÓN TEMPORAL DE DESARROLLO. NO es comunicación distribuida.
 *
 * Es un Singleton en memoria con propiedades observables de JavaFX: solo funciona si el Operador
 * y la Pantalla Pública corren en el MISMO proceso (el menú de pruebas). Cuando las aplicaciones
 * sean independientes, la Pantalla Pública recibirá los TurnoLlamado desde el servidor por socket
 * y este puente se reemplazará (NotificadorLlamados ya aísla el punto de cambio).
 * Al abrirse, la Pantalla Pública precarga su historial desde la BD (cargarHistorial).
 */
public class EstadoPublico {

    private static final int MAXIMO_HISTORIAL = 10;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final EstadoPublico INSTANCIA = new EstadoPublico();

    private final StringProperty turnoLlamado = new SimpleStringProperty("---");
    private final StringProperty ventanillaLlamada = new SimpleStringProperty("-");
    private final ObservableList<String> historial = FXCollections.observableArrayList();

    private EstadoPublico() {
    }

    public static EstadoPublico getInstancia() {
        return INSTANCIA;
    }

    public void publicar(TurnoLlamado llamado) {
        enHiloFx(() -> {
            turnoLlamado.set(llamado.codigo());
            ventanillaLlamada.set(String.valueOf(llamado.numeroVentanilla()));
            historial.add(0, describir(llamado));
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
            historial.setAll(llamados.stream().map(EstadoPublico::describir).toList());
        });
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

    private static String describir(TurnoLlamado llamado) {
        return llamado.codigo() + "  →  Ventanilla " + llamado.numeroVentanilla()
                + "  (" + llamado.fechaHoraLlamado().format(FORMATO_HORA) + ")";
    }

    private static void enHiloFx(Runnable accion) {
        if (Platform.isFxApplicationThread()) {
            accion.run();
        } else {
            Platform.runLater(accion);
        }
    }
}
