package com.turnos.controlador;

import com.turnos.modelo.Rol;
import com.turnos.modelo.Usuario;
import com.turnos.util.Paneles;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

public class AdministradorController {

    @FXML private Node login;
    @FXML private LoginController loginController;
    @FXML private VBox panelAdmin;
    @FXML private Label lblBienvenida;

    @FXML
    private void initialize() {
        loginController.configurar(Rol.ADMIN, "Acceso para administradores", this::alAutenticar);
        Paneles.mostrarSolo(login, login, panelAdmin);
    }

    private void alAutenticar(Usuario administrador) {
        lblBienvenida.setText("Bienvenido(a), " + administrador.nombreCompleto());
        Paneles.mostrarSolo(panelAdmin, login, panelAdmin);
    }

    @FXML
    private void cerrarSesion() {
        loginController.limpiar();
        Paneles.mostrarSolo(login, login, panelAdmin);
    }
}
