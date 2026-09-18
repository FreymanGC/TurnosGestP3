package com.turnos.controlador;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

public class GeneradoraTurnosController {

    @FXML private Label lblTurnoGenerado;

    @FXML
    private void generarTurnoInformacion() {
        mostrarTurnoDeEjemplo("A");
    }

    @FXML
    private void generarTurnoTramites() {
        mostrarTurnoDeEjemplo("B");
    }

    @FXML
    private void generarTurnoPagos() {
        mostrarTurnoDeEjemplo("C");
    }

    @FXML
    private void generarTurnoPreferencial() {
        mostrarTurnoDeEjemplo("D");
    }

    private void mostrarTurnoDeEjemplo(String prefijoServicio) {
        String turno = prefijoServicio + String.format("%03d", (int) (Math.random() * 999) + 1);
        lblTurnoGenerado.setText(turno);
    }
}
