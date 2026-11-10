package com.turnos;

import com.turnos.cliente.ClienteRed;
import com.turnos.util.Ventanas;
import javafx.application.Application;
import javafx.stage.Stage;


public class AdministradorApp extends Application {

    @Override
    public void start(Stage ventana) {
        Ventanas.mostrar(ventana, "/fxml/Administrador.fxml", "Administración");
    }

    /** Al cerrar la ventana se cierra el socket; el servidor libera la sesión y la ventanilla. */
    @Override
    public void stop() {
        ClienteRed.getInstancia().desconectar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
