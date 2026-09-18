package com.turnos.controlador;

import com.turnos.servicio.EstadoPublico;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.VBox;

public class OperadorController {

    @FXML private VBox panelLogin;
    @FXML private BorderPane panelOperador;

    @FXML private TextField txtUsuario;
    @FXML private PasswordField txtContrasena;
    @FXML private Label lblError;

    @FXML private Label lblFuncionario;
    @FXML private Label lblVentanilla;
    @FXML private Label lblEstadoVentanilla;
    @FXML private Label lblTurnoActual;
    @FXML private ListView<String> lstHistorial;

    private final ObservableList<String> historial = FXCollections.observableArrayList();
    private boolean disponible = true;
    private final String numeroVentanilla = "1";

    @FXML
    private void initialize() {
        lstHistorial.setItems(historial);
    }

    @FXML
    private void iniciarSesion() {
        String usuario = txtUsuario.getText();
        String contrasena = txtContrasena.getText();

        if (usuario == null || usuario.isBlank() || contrasena == null || contrasena.isBlank()) {
            lblError.setText("Debe ingresar usuario y contraseña.");
            return;
        }

        lblError.setText("");
        lblFuncionario.setText("Funcionario: " + usuario);
        lblVentanilla.setText("Ventanilla: " + numeroVentanilla);

        panelLogin.setVisible(false);
        panelLogin.setManaged(false);
        panelOperador.setVisible(true);
        panelOperador.setManaged(true);
    }

    @FXML
    private void solicitarSiguienteTurno() {
        String turnoEjemplo = "A" + String.format("%03d", (int) (Math.random() * 999) + 1);
        lblTurnoActual.setText(turnoEjemplo);
        historial.add(0, turnoEjemplo + " atendido en esta ventanilla");
        EstadoPublico.getInstancia().llamarTurno(turnoEjemplo, numeroVentanilla);
    }

    @FXML
    private void finalizarAtencion() {
        lblTurnoActual.setText("---");
    }

    @FXML
    private void volverALlamar() {
        EstadoPublico.getInstancia().llamarTurno(lblTurnoActual.getText(), numeroVentanilla);
    }

    @FXML
    private void alternarDisponibilidad() {
        disponible = !disponible;
        lblEstadoVentanilla.setText("Estado: " + (disponible ? "Disponible" : "No disponible"));
    }
}
