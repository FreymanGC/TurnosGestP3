package com.turnos.controlador;

import com.turnos.cliente.ClienteRed;
import com.turnos.cliente.ObservadorServidor;
import com.turnos.cliente.ServicioRemoto;
import com.turnos.cliente.TurnoRemoto;
import com.turnos.modelo.Servicio;
import com.turnos.modelo.Turno;
import com.turnos.protocolo.Mensaje;
import com.turnos.protocolo.TipoMensaje;
import com.turnos.util.TareaAsincrona;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class GeneradoraTurnosController implements ObservadorServidor {

    private static final int COLUMNAS = 2;
    private static final String SIN_TURNO = "-";
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    @FXML private GridPane gridServicios;
    @FXML private Label lblMensaje;
    @FXML private Label lblTurnoGenerado;
    @FXML private Label lblDetalleTurno;

    private final ServicioRemoto servicioService = new ServicioRemoto();
    private final TurnoRemoto turnoService = new TurnoRemoto();

    @FXML
    private void initialize() {
        ClienteRed.getInstancia().agregarObservador(this);
        cargarServicios();
    }

    private void cargarServicios() {
        TareaAsincrona.ejecutar(servicioService::listarActivos, this::construirBotones, lblMensaje::setText);
    }

    /** Si el administrador cambia los servicios, los botones se actualizan solos. */
    @Override
    public void alRecibirEvento(Mensaje evento) {
        if (evento.getTipo() == TipoMensaje.EVENTO_SERVICIOS_ACTUALIZADOS) {
            Platform.runLater(this::cargarServicios);
        }
    }

    @Override
    public void alPerderConexion() {
        Platform.runLater(() -> lblMensaje.setText(
                "Se perdió la conexion con el servidor. Se reconectara al seleccionar un servicio."));
    }

    private void construirBotones(List<Servicio> servicios) {
        gridServicios.getChildren().clear();
        for (int i = 0; i < servicios.size(); i++) {
            Servicio servicio = servicios.get(i);
            Button boton = new Button(servicio.prefijo() + " · " + servicio.nombre());
            boton.getStyleClass().add(servicio.esPreferencial() ? "boton-preferencial" : "boton-principal");
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
