package com.turnos.controlador;

import com.turnos.modelo.Servicio;
import com.turnos.modelo.Turno;
import com.turnos.servicio.ServicioService;
import com.turnos.servicio.TurnoService;
import com.turnos.util.TareaAsincrona;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class GeneradoraTurnosController {

    private static final int COLUMNAS = 2;
    private static final String SIN_TURNO = "-";
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private GridPane gridServicios;
    @FXML private Label lblMensaje;
    @FXML private Label lblTurnoGenerado;
    @FXML private Label lblDetalleTurno;

    private final ServicioService servicioService = new ServicioService();
    private final TurnoService turnoService = new TurnoService();

    @FXML
    private void initialize() {
        TareaAsincrona.ejecutar(servicioService::listarActivos, this::construirBotones, lblMensaje::setText);
    }

    private void construirBotones(List<Servicio> servicios) {
        gridServicios.getChildren().clear();
        for (int i = 0; i < servicios.size(); i++) {
            Servicio servicio = servicios.get(i);
            Button boton = new Button(servicio.nombre());
            boton.getStyleClass().add("boton-principal");
            boton.setMaxWidth(Double.MAX_VALUE);
            boton.setOnAction(evento -> generarTurno(servicio));
            gridServicios.add(boton, i % COLUMNAS, i / COLUMNAS);
        }
    }

    private void generarTurno(Servicio servicio) {
        lblMensaje.setText("");
        TareaAsincrona.ejecutar(
                () -> turnoService.generarTurno(servicio),
                turno -> mostrarTurno(turno, servicio),
                mensaje -> {
                    lblTurnoGenerado.setText(SIN_TURNO);
                    lblDetalleTurno.setText("");
                    lblMensaje.setText(mensaje);
                },
                gridServicios);
    }

    private void mostrarTurno(Turno turno, Servicio servicio) {
        lblTurnoGenerado.setText(turno.codigo());
        lblDetalleTurno.setText(servicio.nombre() + "  ·  " + turno.fechaHoraGeneracion().format(FORMATO_HORA));
    }
}
