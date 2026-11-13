package com.turnos.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.io.IOException;
import java.io.UncheckedIOException;


public final class Ventanas {

    private static final String ICONO = "/img/icono.png";

    private Ventanas() {
    }

    public static void mostrar(Stage ventana, String rutaFxml, String titulo) {
        try {
            Parent raiz = FXMLLoader.load(Ventanas.class.getResource(rutaFxml));
            Scene escena = new Scene(raiz);
            ventana.getIcons().setAll(new Image(Ventanas.class.getResourceAsStream(ICONO)));
            ventana.setTitle(titulo);
            ventana.setScene(escena);
            ventana.show();
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo cargar " + rutaFxml, e);
        }
    }
}