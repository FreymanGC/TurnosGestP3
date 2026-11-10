package com.turnos;

import com.turnos.cliente.ClienteRed;
import com.turnos.util.Ventanas;
import javafx.application.Application;
import javafx.stage.Stage;

public class GeneradoraApp extends Application {

    @Override
    public void start(Stage ventana) {
        Ventanas.mostrar(ventana, "/fxml/GeneradoraTurnos.fxml", "Generadora de Turnos");
    }
@Override
    public void stop() {
        ClienteRed.getInstancia().desconectar();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
