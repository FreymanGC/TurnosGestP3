package com.turnos.controlador;

import com.turnos.servicio.EstadoPublico;
import com.turnos.servicio.TurnoService;
import com.turnos.util.TareaAsincrona;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.beans.binding.Bindings;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.util.Duration;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

public class PantallaPublicaController {

    @FXML private Label lblHora;
    @FXML private Label lblTurnoActual;
    @FXML private Label lblVentanilla;
    @FXML private Label lblAviso;
    @FXML private ListView<String> lstHistorial;

    private static final int LIMITE_HISTORIAL = 10;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private void initialize() {
        iniciarReloj();

        EstadoPublico estado = EstadoPublico.getInstancia();
        lblTurnoActual.textProperty().bind(estado.turnoLlamadoProperty());
        lblVentanilla.textProperty().bind(Bindings.concat("Ventanilla: ", estado.ventanillaLlamadaProperty()));
        lstHistorial.setItems(estado.getHistorial());
        precargarHistorialDesdeBD(estado);
    }

    private void precargarHistorialDesdeBD(EstadoPublico estado) {
        TareaAsincrona.ejecutar(
                () -> new TurnoService().ultimosLlamados(LIMITE_HISTORIAL),
                estado::cargarHistorial,
                mensaje -> lblAviso.setText("No se pudo cargar el historial: " + mensaje));
    }

    private void iniciarReloj() {
        Timeline reloj = new Timeline(
                new KeyFrame(Duration.ZERO, e -> lblHora.setText(LocalTime.now().format(FORMATO_HORA))),
                new KeyFrame(Duration.seconds(1))
        );
        reloj.setCycleCount(Timeline.INDEFINITE);
        reloj.play();
    }
}
