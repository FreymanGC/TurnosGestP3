package com.turnos.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;

/** Carga una vista FXML con la hoja de estilos compartida y la muestra en la ventana dada. */
public final class Ventanas {

    private static final String ESTILOS = "/css/estilos.css";

    private Ventanas() {
    }

    public static void mostrar(Stage ventana, String rutaFxml, String titulo) {
        try {
            Parent raiz = FXMLLoader.load(Ventanas.class.getResource(rutaFxml));
            Scene escena = new Scene(raiz);
            escena.getStylesheets().add(Ventanas.class.getResource(ESTILOS).toExternalForm());
            ventana.setTitle(titulo);
            ventana.setScene(escena);
            ventana.show();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar " + rutaFxml, e);
        }
    }
}
