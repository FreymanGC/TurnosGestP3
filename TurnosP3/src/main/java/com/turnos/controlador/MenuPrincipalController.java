package com.turnos.controlador;

import com.turnos.MainApp;
import javafx.fxml.FXML;

public class MenuPrincipalController {

    @FXML
    private void abrirPantallaPublica() {
        MainApp.abrirVentana("/fxml/PantallaPublica.fxml", "Pantalla Pública");
    }

    @FXML
    private void abrirGeneradoraTurnos() {
        MainApp.abrirVentana("/fxml/GeneradoraTurnos.fxml", "Generadora de Turnos");
    }

    @FXML
    private void abrirOperador() {
        MainApp.abrirVentana("/fxml/Operador.fxml", "Operador");
    }

    @FXML
    private void abrirAdministrador() {
        MainApp.abrirVentana("/fxml/Administrador.fxml", "Administración");
    }
}
