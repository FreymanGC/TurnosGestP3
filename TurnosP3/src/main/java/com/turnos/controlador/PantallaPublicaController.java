package com.turnos.controlador;

import com.turnos.servicio.EstadoPublico;
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
    @FXML private ListView<String> lstHistorial;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML
    private void initialize() {
        iniciarReloj();

        EstadoPublico estado = EstadoPublico.getInstancia();
        lblTurnoActual.textProperty().bind(estado.turnoLlamadoProperty());
        lblVentanilla.textProperty().bind(Bindings.concat("Ventanilla: ", estado.ventanillaLlamadaProperty()));
        lstHistorial.setItems(estado.getHistorial());
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
